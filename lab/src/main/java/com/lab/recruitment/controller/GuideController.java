package com.lab.recruitment.controller;

import com.lab.recruitment.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/guide")
public class GuideController {

    @GetMapping("/options")
    public Result<List<Map<String, Object>>> getGuideOptions() {
        List<Map<String, Object>> options = new ArrayList<>();

        options.add(option(
                1,
                "backend",
                "后端开发与服务架构",
                "Java后端工程师",
                "负责业务系统、接口服务和数据库设计，适合喜欢逻辑建模和系统实现的同学。",
                list("黑马程序员 Java 入门", "CS61B Data Structures", "Spring Boot 官方文档"),
                list("《Effective Java》", "《深入理解 Java 虚拟机》", "《高性能 MySQL》"),
                list("蓝桥杯软件类 Java 组", "中国大学生服务外包创新创业大赛", "ACM 程序设计竞赛"),
                list("Oracle Certified Professional Java SE", "软考中级软件设计师")
        ));

        options.add(option(
                2,
                "frontend",
                "前端开发与交互实现",
                "Web前端工程师",
                "聚焦页面设计、交互实现和工程化构建，适合喜欢把创意变成可视化作品的同学。",
                list("MDN Web Docs", "Vue 官方文档", "freeCodeCamp Responsive Web Design"),
                list("《JavaScript 高级程序设计》", "《CSS 揭秘》", "《Vue.js 设计与实现》"),
                list("中国大学生计算机设计大赛 Web 应用开发", "蓝桥杯 Web 应用开发组"),
                list("W3C HTML5 开发者认证", "Google UX Design Certificate")
        ));

        options.add(option(
                3,
                "ai",
                "人工智能与算法研究",
                "AI算法工程师",
                "学习机器学习、深度学习和模型应用，适合对图像、文本和智能系统感兴趣的同学。",
                list("Andrew Ng Machine Learning", "CS231n", "《动手学深度学习》课程"),
                list("《机器学习》", "《深度学习》", "《统计学习方法》"),
                list("Kaggle 竞赛", "全国大学生数学建模竞赛", "中国高校计算机大赛人工智能创意赛"),
                list("TensorFlow Developer Certificate", "CDA 数据分析师")
        ));

        options.add(option(
                4,
                "embedded",
                "嵌入式与物联网开发",
                "嵌入式工程师",
                "围绕单片机、驱动和硬件交互展开，适合喜欢软硬件结合和底层控制的同学。",
                list("STM32 官方入门课程", "正点原子 STM32 教程", "CSAPP"),
                list("《C 程序设计语言》", "《深入理解计算机系统》", "《Linux 设备驱动开发详解》"),
                list("全国大学生电子设计竞赛", "全国大学生智能汽车竞赛"),
                list("ARM Accredited Engineer", "软考嵌入式系统设计师")
        ));

        options.add(option(
                5,
                "security",
                "网络安全与攻防实践",
                "安全工程师",
                "关注 Web 安全、系统安全和攻防演练，适合喜欢分析风险和排查漏洞的同学。",
                list("CTF Wiki", "OWASP Web Security Testing Guide", "NJU ICS 计算机系统基础"),
                list("《白帽子讲 Web 安全》", "《Web 安全攻防》", "《网络是怎样连接的》"),
                list("全国大学生信息安全竞赛", "强网杯", "CTF 校赛/省赛"),
                list("CISP", "CompTIA Security+")
        ));

        options.add(option(
                6,
                "bigdata",
                "大数据平台与数据工程",
                "大数据开发工程师",
                "围绕海量数据处理、分布式计算和数据仓库建设，适合喜欢工程规模感的同学。",
                list("Spark 官方文档", "Hadoop 实战课程", "MIT 6.830 Database Systems"),
                list("《Hadoop 权威指南》", "《Spark 快速大数据分析》", "《数据库系统概念》"),
                list("中国高校计算机大赛大数据挑战赛", "全国大学生大数据技能竞赛"),
                list("Cloudera Data Platform Generalist", "阿里云大数据工程师 ACP")
        ));

        options.add(option(
                7,
                "analysis",
                "数据分析与商业洞察",
                "数据分析师",
                "通过统计分析、可视化和业务解读，把数据转化为能落地的判断和建议。",
                list("Python for Everybody", "Kaggle Learn", "Google Data Analytics"),
                list("《利用 Python 进行数据分析》", "《深入浅出统计学》", "《数据分析实战》"),
                list("MathorCup 高校数学建模挑战赛", "全国市场调查与分析大赛", "Kaggle 入门赛"),
                list("CDA 数据分析师", "Google Data Analytics Certificate")
        ));

        options.add(option(
                8,
                "product",
                "产品策划与需求设计",
                "产品经理",
                "负责需求分析、方案设计和协作推进，适合喜欢沟通、拆解问题和组织资源的同学。",
                list("人人都是产品经理专栏", "Coursera Digital Product Management", "产品经理入门课程"),
                list("《启示录》", "《结网》", "《人人都是产品经理》"),
                list("互联网+ 大学生创新创业大赛", "全国大学生电子商务三创赛", "商业分析挑战赛"),
                list("NPDP", "软考系统集成项目管理工程师")
        ));

        options.add(option(
                9,
                "design",
                "UI/UX 设计与体验优化",
                "交互设计师",
                "兼顾视觉表达与用户体验，适合喜欢审美表达、信息组织和交互细节的同学。",
                list("Figma 官方入门", "Google UX Design", "站酷高赞设计课程"),
                list("《写给大家看的设计书》", "《点石成金》", "《用户体验要素》"),
                list("中国大学生计算机设计大赛数媒设计类", "全国大学生广告艺术大赛"),
                list("Adobe Certified Professional", "Google UX Design Certificate")
        ));

        options.add(option(
                10,
                "qa",
                "测试开发与质量保障",
                "测试开发工程师",
                "通过自动化测试、接口验证和质量流程建设，保障系统稳定交付。",
                list("Selenium 官方文档", "Postman Learning Center", "软件测试基础课程"),
                list("《Google 软件测试之道》", "《软件测试的艺术》", "《持续交付》"),
                list("全国软件测试大赛", "软件质量与自动化测试技能赛"),
                list("ISTQB Foundation Level", "软考软件评测师")
        ));

        options.add(option(
                11,
                "cloud",
                "运维自动化与云计算",
                "云平台工程师",
                "关注部署、监控、容器和云平台运维，适合喜欢系统稳定性与自动化效率的同学。",
                list("Docker 官方文档", "Kubernetes 官方教程", "Linux Foundation 入门课程"),
                list("《Kubernetes 权威指南》", "《UNIX/Linux 系统管理技术手册》", "《凤凰项目》"),
                list("全国大学生云计算应用创新大赛", "中国软件杯云原生方向赛题"),
                list("AWS Cloud Practitioner", "阿里云 ACA 云计算")
        ));

        options.add(option(
                12,
                "game",
                "游戏开发与实时交互",
                "游戏客户端/引擎工程师",
                "结合图形渲染、交互逻辑和性能优化，适合热爱创作和实时系统的同学。",
                list("Unity Learn", "Unreal Engine 官方学习", "Game Programming Patterns"),
                list("《游戏编程模式》", "《Unity 3D 游戏开发》", "《计算机图形学基础》"),
                list("中国大学生计算机设计大赛游戏开发类", "高校游戏创意设计大赛"),
                list("Unity Certified User", "Unity Certified Associate")
        ));

        return Result.success(options);
    }

    private Map<String, Object> option(
            int id,
            String iconKey,
            String intention,
            String career,
            String description,
            List<String> courses,
            List<String> books,
            List<String> competitions,
            List<String> certificates) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("id", id);
        option.put("iconKey", iconKey);
        option.put("intention", intention);
        option.put("career", career);
        option.put("description", description);
        option.put("courses", courses);
        option.put("books", books);
        option.put("competitions", competitions);
        option.put("certificates", certificates);
        return option;
    }

    private List<String> list(String... values) {
        return Arrays.asList(values);
    }
}
