package com.lab.recruitment.service.impl;

import com.lab.recruitment.dto.JudgeCaseDTO;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CodeJudgeService {

    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration ENVIRONMENT_CACHE_TTL = Duration.ofSeconds(45);
    private static final int MAX_STREAM_CAPTURE_BYTES = 64 * 1024;

    @Value("${judge.python-command:python}")
    private String pythonCommand;

    @Value("${judge.c-compiler-command:gcc}")
    private String cCompilerCommand;

    @Value("${judge.cpp-compiler-command:g++}")
    private String cppCompilerCommand;

    @Value("${judge.java-compiler-command:javac}")
    private String javaCompilerCommand;

    @Value("${judge.java-command:java}")
    private String javaCommand;

    @Value("${judge.work-dir:}")
    private String judgeWorkDir;

    private volatile Map<String, Map<String, Object>> cachedEnvironmentDetails;
    private volatile long cachedEnvironmentExpireAt;

    public Map<String, Boolean> getEnvironmentStatus() {
        Map<String, Boolean> status = new LinkedHashMap<>();
        getEnvironmentDetails().forEach((key, value) -> status.put(key, Boolean.TRUE.equals(value.get("available"))));
        return status;
    }

    public Map<String, Map<String, Object>> getEnvironmentDetails() {
        long now = System.currentTimeMillis();
        Map<String, Map<String, Object>> cached = cachedEnvironmentDetails;
        if (cached != null && now < cachedEnvironmentExpireAt) {
            return cached;
        }

        synchronized (this) {
            cached = cachedEnvironmentDetails;
            if (cached != null && now < cachedEnvironmentExpireAt) {
                return cached;
            }

            Map<String, Map<String, Object>> details = loadEnvironmentDetails();
            cachedEnvironmentDetails = details;
            cachedEnvironmentExpireAt = now + ENVIRONMENT_CACHE_TTL.toMillis();
            return details;
        }
    }

    private Map<String, Map<String, Object>> loadEnvironmentDetails() {
        Map<String, Map<String, Object>> details = new LinkedHashMap<>();

        String resolvedC = resolveCommand(cCompilerCommand, "--version", getCCompilerCandidates());
        details.put("c", buildEnvironmentDetail(
                "C",
                cCompilerCommand,
                resolvedC,
                isCommandAvailable(resolvedC, "--version"),
                "Need a working C compiler such as GCC or Clang"
        ));

        String resolvedCpp = resolveCommand(cppCompilerCommand, "--version", getCppCompilerCandidates());
        details.put("cpp", buildEnvironmentDetail(
                "C++",
                cppCompilerCommand,
                resolvedCpp,
                isCommandAvailable(resolvedCpp, "--version"),
                "Need a working C++ compiler such as G++ or Clang++"
        ));

        String resolvedJavaCompiler = resolveCommand(javaCompilerCommand, "-version", List.of("javac"));
        String resolvedJavaRuntime = resolveCommand(javaCommand, "-version", List.of("java"));
        details.put("java", buildJavaEnvironmentDetail(
                resolvedJavaCompiler,
                resolvedJavaRuntime
        ));

        String resolvedPython = resolveCommand(pythonCommand, "--version", getPythonCandidates());
        details.put("python", buildEnvironmentDetail(
                "Python",
                pythonCommand,
                resolvedPython,
                isCommandAvailable(resolvedPython, "--version"),
                "Need a runnable Python interpreter"
        ));

        return details;
    }

    public JudgeResult judge(String language, String code, List<JudgeCaseDTO> judgeCases) {
        if (!StringUtils.hasText(language)) {
            return unavailable("Please select a programming language");
        }
        if (!StringUtils.hasText(code)) {
            return unavailable("Please submit code");
        }
        if (judgeCases == null || judgeCases.isEmpty()) {
            return unavailable("Judge cases are not configured");
        }

        switch (language) {
            case "java":
                return judgeJava(code, judgeCases);
            case "python":
                return judgePython(code, judgeCases);
            case "c":
                return judgeC(code, judgeCases);
            case "cpp":
                return judgeCpp(code, judgeCases);
            default:
                return unavailable("Unsupported language: " + language);
        }
    }

    public RunResult run(String language, String code, String input) {
        if (!StringUtils.hasText(language)) {
            return runUnavailable("Please select a programming language");
        }
        if (!StringUtils.hasText(code)) {
            return runUnavailable("Please submit code");
        }

        switch (language) {
            case "java":
                return runJava(code, input);
            case "python":
                return runPython(code, input);
            case "c":
                return runC(code, input);
            case "cpp":
                return runCpp(code, input);
            default:
                return runUnavailable("Unsupported language: " + language);
        }
    }

    public BigDecimal calculateScore(Integer fullScore, JudgeResult judgeResult) {
        if (fullScore == null || fullScore <= 0 || judgeResult == null || judgeResult.getTotalCount() <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(fullScore)
                .multiply(BigDecimal.valueOf(judgeResult.getPassedCount()))
                .divide(BigDecimal.valueOf(judgeResult.getTotalCount()), 2, RoundingMode.HALF_UP);
    }

    private RunResult runJava(String code, String input) {
        String compiler = resolveCommand(javaCompilerCommand, "-version", List.of("javac"));
        String runtime = resolveCommand(javaCommand, "-version", List.of("java"));
        if (!isCommandAvailable(compiler, "-version") || !isCommandAvailable(runtime, "-version")) {
            return runUnavailable("Java judge environment is not available on the server");
        }

        try {
            Path workDir = createJudgeWorkDir("judge-java-run-");
            try {
                Files.writeString(workDir.resolve("Main.java"), code, StandardCharsets.UTF_8);
                ProcessResult compileResult = runProcess(new String[]{compiler, "Main.java"}, workDir, null);
                if (compileResult.exitCode != 0) {
                    return failedRun("compile_error", "Compilation failed", compileResult.stderr, compileResult.stdout);
                }
                return buildRunResult(runProcess(new String[]{runtime, "Main"}, workDir, input));
            } finally {
                deleteDirectoryQuietly(workDir);
            }
        } catch (Exception e) {
            return runUnavailable("Java judge failed: " + e.getMessage());
        }
    }

    private RunResult runPython(String code, String input) {
        String runtime = resolveCommand(pythonCommand, "--version", getPythonCandidates());
        if (!isCommandAvailable(runtime, "--version")) {
            return runUnavailable("Python judge environment is not available on the server");
        }

        try {
            Path workDir = createJudgeWorkDir("judge-python-run-");
            try {
                Files.writeString(workDir.resolve("main.py"), code, StandardCharsets.UTF_8);
                return buildRunResult(runProcess(new String[]{runtime, "main.py"}, workDir, input));
            } finally {
                deleteDirectoryQuietly(workDir);
            }
        } catch (Exception e) {
            return runUnavailable("Python judge failed: " + e.getMessage());
        }
    }

    private RunResult runC(String code, String input) {
        String compiler = resolveCommand(cCompilerCommand, "--version", getCCompilerCandidates());
        if (!isCommandAvailable(compiler, "--version")) {
            return runUnavailable("C judge environment is not available on the server");
        }

        try {
            Path workDir = createJudgeWorkDir("judge-c-run-");
            try {
                Path targetFile = workDir.resolve("main.exe");
                Files.writeString(workDir.resolve("main.c"), code, StandardCharsets.UTF_8);
                ProcessResult compileResult = runProcess(
                        new String[]{compiler, "main.c", "-O2", "-o", targetFile.toString()},
                        workDir,
                        null
                );
                if (compileResult.exitCode != 0) {
                    return failedRun("compile_error", "Compilation failed", compileResult.stderr, compileResult.stdout);
                }
                return buildRunResult(runProcess(new String[]{targetFile.toString()}, workDir, input));
            } finally {
                deleteDirectoryQuietly(workDir);
            }
        } catch (Exception e) {
            return runUnavailable("C judge failed: " + e.getMessage());
        }
    }

    private RunResult runCpp(String code, String input) {
        String compiler = resolveCommand(cppCompilerCommand, "--version", getCppCompilerCandidates());
        if (!isCommandAvailable(compiler, "--version")) {
            return runUnavailable("C++ judge environment is not available on the server");
        }

        try {
            Path workDir = createJudgeWorkDir("judge-cpp-run-");
            try {
                Path targetFile = workDir.resolve("main.exe");
                Files.writeString(workDir.resolve("main.cpp"), code, StandardCharsets.UTF_8);
                ProcessResult compileResult = runProcess(
                        new String[]{compiler, "main.cpp", "-O2", "-std=c++17", "-o", targetFile.toString()},
                        workDir,
                        null
                );
                if (compileResult.exitCode != 0) {
                    return failedRun("compile_error", "Compilation failed", compileResult.stderr, compileResult.stdout);
                }
                return buildRunResult(runProcess(new String[]{targetFile.toString()}, workDir, input));
            } finally {
                deleteDirectoryQuietly(workDir);
            }
        } catch (Exception e) {
            return runUnavailable("C++ judge failed: " + e.getMessage());
        }
    }

    private JudgeResult judgeJava(String code, List<JudgeCaseDTO> judgeCases) {
        String compiler = resolveCommand(javaCompilerCommand, "-version", List.of("javac"));
        String runtime = resolveCommand(javaCommand, "-version", List.of("java"));
        if (!isCommandAvailable(compiler, "-version") || !isCommandAvailable(runtime, "-version")) {
            return unavailable("Java judge environment is not available on the server");
        }

        try {
            Path workDir = createJudgeWorkDir("judge-java-");
            try {
                Files.writeString(workDir.resolve("Main.java"), code, StandardCharsets.UTF_8);
                ProcessResult compileResult = runProcess(new String[]{compiler, "Main.java"}, workDir, null);
                if (compileResult.exitCode != 0) {
                    return failedWithMessage("Compilation failed", compileResult.stderr);
                }
                return runJudgeCases(workDir, new String[]{runtime, "Main"}, judgeCases);
            } finally {
                deleteDirectoryQuietly(workDir);
            }
        } catch (Exception e) {
            return unavailable("Java judge failed: " + e.getMessage());
        }
    }

    private JudgeResult judgePython(String code, List<JudgeCaseDTO> judgeCases) {
        String runtime = resolveCommand(pythonCommand, "--version", getPythonCandidates());
        if (!isCommandAvailable(runtime, "--version")) {
            return unavailable("Python judge environment is not available on the server");
        }

        try {
            Path workDir = createJudgeWorkDir("judge-python-");
            try {
                Files.writeString(workDir.resolve("main.py"), code, StandardCharsets.UTF_8);
                return runJudgeCases(workDir, new String[]{runtime, "main.py"}, judgeCases);
            } finally {
                deleteDirectoryQuietly(workDir);
            }
        } catch (Exception e) {
            return unavailable("Python judge failed: " + e.getMessage());
        }
    }

    private JudgeResult judgeC(String code, List<JudgeCaseDTO> judgeCases) {
        String compiler = resolveCommand(cCompilerCommand, "--version", getCCompilerCandidates());
        if (!isCommandAvailable(compiler, "--version")) {
            return unavailable("C judge environment is not available on the server");
        }

        try {
            Path workDir = createJudgeWorkDir("judge-c-");
            try {
                Path targetFile = workDir.resolve("main.exe");
                Files.writeString(workDir.resolve("main.c"), code, StandardCharsets.UTF_8);
                ProcessResult compileResult = runProcess(
                        new String[]{compiler, "main.c", "-O2", "-o", targetFile.toString()},
                        workDir,
                        null
                );
                if (compileResult.exitCode != 0) {
                    return failedWithMessage("Compilation failed", compileResult.stderr);
                }
                return runJudgeCases(workDir, new String[]{targetFile.toString()}, judgeCases);
            } finally {
                deleteDirectoryQuietly(workDir);
            }
        } catch (Exception e) {
            return unavailable("C judge failed: " + e.getMessage());
        }
    }

    private JudgeResult judgeCpp(String code, List<JudgeCaseDTO> judgeCases) {
        String compiler = resolveCommand(cppCompilerCommand, "--version", getCppCompilerCandidates());
        if (!isCommandAvailable(compiler, "--version")) {
            return unavailable("C++ judge environment is not available on the server");
        }

        try {
            Path workDir = createJudgeWorkDir("judge-cpp-");
            try {
                Path targetFile = workDir.resolve("main.exe");
                Files.writeString(workDir.resolve("main.cpp"), code, StandardCharsets.UTF_8);
                ProcessResult compileResult = runProcess(
                        new String[]{compiler, "main.cpp", "-O2", "-std=c++17", "-o", targetFile.toString()},
                        workDir,
                        null
                );
                if (compileResult.exitCode != 0) {
                    return failedWithMessage("Compilation failed", compileResult.stderr);
                }
                return runJudgeCases(workDir, new String[]{targetFile.toString()}, judgeCases);
            } finally {
                deleteDirectoryQuietly(workDir);
            }
        } catch (Exception e) {
            return unavailable("C++ judge failed: " + e.getMessage());
        }
    }

    private JudgeResult runJudgeCases(Path workDir, String[] command, List<JudgeCaseDTO> judgeCases) throws Exception {
        int passedCount = 0;
        StringBuilder detail = new StringBuilder();

        for (int index = 0; index < judgeCases.size(); index++) {
            JudgeCaseDTO judgeCase = judgeCases.get(index);
            ProcessResult runResult = runProcess(command, workDir, judgeCase.getInput());
            if (runResult.timedOut) {
                detail.append("Case ").append(index + 1).append(" timed out; ");
                continue;
            }
            if (runResult.exitCode != 0) {
                detail.append("Case ").append(index + 1).append(" failed: ")
                        .append(trimMessage(runResult.stderr))
                        .append("; ");
                continue;
            }
            if (normalizeOutput(runResult.stdout).equals(normalizeOutput(judgeCase.getOutput()))) {
                passedCount++;
            } else {
                detail.append("Case ").append(index + 1).append(" wrong answer; ");
            }
        }

        JudgeResult result = new JudgeResult();
        result.setAvailable(true);
        result.setPassedCount(passedCount);
        result.setTotalCount(judgeCases.size());
        result.setSuccess(passedCount == judgeCases.size());
        result.setMessage(detail.length() == 0 ? "All judge cases passed" : detail.toString().trim());
        return result;
    }

    private ProcessResult runProcess(String[] command, Path workDir, String input) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workDir.toFile());
        String tempDir = getJudgeBaseDir().toString();
        processBuilder.environment().put("TMP", tempDir);
        processBuilder.environment().put("TEMP", tempDir);
        processBuilder.environment().put("TMPDIR", tempDir);
        appendCompilerBinDirs(processBuilder.environment());
        Process process = processBuilder.start();

        if (input != null) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(input);
                writer.flush();
            }
        } else {
            process.getOutputStream().close();
        }

        boolean finished = process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        ProcessResult result = new ProcessResult();
        if (!finished) {
            result.timedOut = true;
            process.destroyForcibly();
            result.stdout = readStream(process.getInputStream());
            result.stderr = readStream(process.getErrorStream());
            return result;
        }

        result.exitCode = process.exitValue();
        result.stdout = readStream(process.getInputStream());
        result.stderr = readStream(process.getErrorStream());
        return result;
    }

    private Map<String, Object> buildEnvironmentDetail(String label,
                                                       String configuredCommand,
                                                       String resolvedCommand,
                                                       boolean available,
                                                       String unavailableHint) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("label", label);
        detail.put("available", available);
        detail.put("configuredCommand", safeCommand(configuredCommand));
        detail.put("resolvedCommand", safeCommand(resolvedCommand));
        detail.put("message", available ? "Ready" : unavailableHint);
        return detail;
    }

    private Map<String, Object> buildJavaEnvironmentDetail(String compilerCommand, String runtimeCommand) {
        boolean compilerAvailable = isCommandAvailable(compilerCommand, "-version");
        boolean runtimeAvailable = isCommandAvailable(runtimeCommand, "-version");
        boolean available = compilerAvailable && runtimeAvailable;

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("label", "Java");
        detail.put("available", available);
        detail.put("configuredCommand", safeCommand(javaCompilerCommand) + " | " + safeCommand(javaCommand));
        detail.put("resolvedCommand", safeCommand(compilerCommand) + " | " + safeCommand(runtimeCommand));
        detail.put("message", available ? "Ready" : "Need both javac and java on the server");
        return detail;
    }

    private String resolveCommand(String configuredCommand, String versionArg, List<String> fallbackCandidates) {
        List<String> candidates = new ArrayList<>();
        if (StringUtils.hasText(configuredCommand)) {
            candidates.add(configuredCommand.trim());
        }
        if (fallbackCandidates != null) {
            candidates.addAll(fallbackCandidates);
        }

        List<String> normalizedCandidates = candidates.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        for (String candidate : normalizedCandidates) {
            if (isCommandAvailable(candidate, versionArg)) {
                return candidate;
            }
        }
        return normalizedCandidates.isEmpty() ? null : normalizedCandidates.get(0);
    }

    private List<String> getPythonCandidates() {
        List<String> candidates = new ArrayList<>();
        candidates.add("python");
        if (isWindows()) {
            candidates.add("C:\\Program Files\\MySQL\\MySQL Workbench 8.0\\python.exe");
            candidates.add("C:\\Program Files\\Python311\\python.exe");
            candidates.add("C:\\Program Files\\Python312\\python.exe");
            candidates.add("C:\\Program Files\\Python313\\python.exe");
            String localAppData = System.getenv("LOCALAPPDATA");
            if (StringUtils.hasText(localAppData)) {
                candidates.add(localAppData + "\\Programs\\Python\\Python311\\python.exe");
                candidates.add(localAppData + "\\Programs\\Python\\Python312\\python.exe");
                candidates.add(localAppData + "\\Programs\\Python\\Python313\\python.exe");
            }
        }
        return candidates;
    }

    private List<String> getCCompilerCandidates() {
        List<String> candidates = new ArrayList<>();
        candidates.add("gcc");
        if (isWindows()) {
            candidates.add("C:\\msys64\\ucrt64\\bin\\gcc.exe");
            candidates.add("C:\\msys64\\mingw64\\bin\\gcc.exe");
            candidates.add("C:\\Program Files\\LLVM\\bin\\clang.exe");
        }
        return candidates;
    }

    private List<String> getCppCompilerCandidates() {
        List<String> candidates = new ArrayList<>();
        candidates.add("g++");
        if (isWindows()) {
            candidates.add("C:\\msys64\\ucrt64\\bin\\g++.exe");
            candidates.add("C:\\msys64\\mingw64\\bin\\g++.exe");
            candidates.add("C:\\Program Files\\LLVM\\bin\\clang++.exe");
        }
        return candidates;
    }

    private boolean isCommandAvailable(String command, String versionArg) {
        if (!StringUtils.hasText(command)) {
            return false;
        }
        try {
            Process process = new ProcessBuilder(command, versionArg).start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private String readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        int read;
        boolean truncated = false;
        while ((read = inputStream.read(buffer, 0, Math.min(buffer.length, MAX_STREAM_CAPTURE_BYTES - total))) != -1) {
            outputStream.write(buffer, 0, read);
            total += read;
            if (total >= MAX_STREAM_CAPTURE_BYTES) {
                truncated = inputStream.read() != -1;
                break;
            }
        }
        String output = outputStream.toString(StandardCharsets.UTF_8);
        return truncated ? output + "\n[output truncated]" : output;
    }

    private JudgeResult unavailable(String message) {
        JudgeResult result = new JudgeResult();
        result.setAvailable(false);
        result.setSuccess(false);
        result.setPassedCount(0);
        result.setTotalCount(0);
        result.setMessage(message);
        return result;
    }

    private RunResult buildRunResult(ProcessResult processResult) {
        RunResult result = new RunResult();
        result.setAvailable(true);
        result.setStdout(processResult.stdout);
        result.setStderr(processResult.stderr);
        result.setTimedOut(processResult.timedOut);
        if (processResult.timedOut) {
            result.setStatus("time_limit_exceeded");
            result.setError("Execution timed out");
            return result;
        }
        if (processResult.exitCode == 0) {
            result.setStatus("success");
            return result;
        }
        result.setStatus("runtime_error");
        result.setError(trimMessage(processResult.stderr));
        return result;
    }

    private RunResult failedRun(String status, String message, String stderr, String stdout) {
        RunResult result = new RunResult();
        result.setAvailable(true);
        result.setStatus(status);
        result.setError(message + ": " + trimMessage(stderr));
        result.setStdout(stdout);
        result.setStderr(stderr);
        return result;
    }

    private RunResult runUnavailable(String message) {
        RunResult result = new RunResult();
        result.setAvailable(false);
        result.setStatus("error");
        result.setError(message);
        return result;
    }

    private JudgeResult failedWithMessage(String prefix, String detail) {
        JudgeResult result = new JudgeResult();
        result.setAvailable(true);
        result.setSuccess(false);
        result.setPassedCount(0);
        result.setTotalCount(0);
        result.setMessage(prefix + ": " + trimMessage(detail));
        return result;
    }

    private String normalizeOutput(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").trim();
    }

    private String trimMessage(String value) {
        if (!StringUtils.hasText(value)) {
            return "No detail";
        }
        String normalized = value.replace("\r\n", " ").replace('\n', ' ').trim();
        return normalized.length() > 180 ? normalized.substring(0, 180) : normalized;
    }

    private String safeCommand(String command) {
        return StringUtils.hasText(command) ? command.trim() : "-";
    }

    private Path createJudgeWorkDir(String prefix) throws IOException {
        Path baseDir = getJudgeBaseDir();
        Files.createDirectories(baseDir);
        return Files.createTempDirectory(baseDir, prefix);
    }

    private Path getJudgeBaseDir() {
        Path baseDir;
        if (StringUtils.hasText(judgeWorkDir)) {
            baseDir = Paths.get(judgeWorkDir.trim());
        } else if (isWindows()) {
            String systemDrive = StringUtils.hasText(System.getenv("SystemDrive")) ? System.getenv("SystemDrive") : "C:";
            baseDir = Paths.get(systemDrive, "lab-judge-temp");
        } else {
            baseDir = Paths.get(System.getProperty("java.io.tmpdir"), "lab-judge-temp");
        }
        return baseDir;
    }

    private void appendCompilerBinDirs(Map<String, String> environment) {
        if (environment == null) {
            return;
        }
        String pathKey = isWindows() ? "Path" : "PATH";
        String currentPath = environment.getOrDefault(pathKey, environment.getOrDefault("PATH", ""));

        List<String> pathEntries = new ArrayList<>();
        if (StringUtils.hasText(currentPath)) {
            pathEntries.add(currentPath);
        }
        addCommandParentDirectory(pathEntries, cCompilerCommand);
        addCommandParentDirectory(pathEntries, cppCompilerCommand);

        String mergedPath = pathEntries.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(File.pathSeparator));
        environment.put(pathKey, mergedPath);
        environment.put("PATH", mergedPath);
    }

    private void addCommandParentDirectory(List<String> pathEntries, String command) {
        if (!StringUtils.hasText(command)) {
            return;
        }
        try {
            Path commandPath = Paths.get(command.trim());
            Path parent = commandPath.getParent();
            if (parent != null) {
                pathEntries.add(parent.toString());
            }
        } catch (Exception ignored) {
        }
    }

    private void deleteDirectoryQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.walk(path)
                    .sorted((left, right) -> right.compareTo(left))
                    .forEach(item -> {
                        try {
                            Files.deleteIfExists(item);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    @Data
    public static class JudgeResult {
        private boolean available;
        private boolean success;
        private int passedCount;
        private int totalCount;
        private String message;
    }

    @Data
    public static class RunResult {
        private boolean available;
        private boolean timedOut;
        private String status;
        private String stdout;
        private String stderr;
        private String error;
    }

    private static class ProcessResult {
        private int exitCode;
        private boolean timedOut;
        private String stdout;
        private String stderr;
    }
}
