package com.lab.recruitment.utils;

import lombok.Data;

@Data
public class Result<T> {
    public static final int LEGACY_SUCCESS_CODE = 200;
    public static final int API_SUCCESS_CODE = 0;

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(LEGACY_SUCCESS_CODE);
        result.setMessage("成功");
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(LEGACY_SUCCESS_CODE);
        result.setMessage("成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(LEGACY_SUCCESS_CODE);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> apiSuccess() {
        Result<T> result = new Result<>();
        result.setCode(API_SUCCESS_CODE);
        result.setMessage("成功");
        return result;
    }

    public static <T> Result<T> apiSuccess(T data) {
        Result<T> result = new Result<>();
        result.setCode(API_SUCCESS_CODE);
        result.setMessage("成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> apiSuccess(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(API_SUCCESS_CODE);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error() {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage("失败");
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
