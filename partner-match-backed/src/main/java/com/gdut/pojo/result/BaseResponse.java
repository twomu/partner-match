package com.gdut.pojo.result;


import com.gdut.common.ErrorCode;

public class BaseResponse<T>  {
    private int code;
    private T data;
    private String message;
    private String description;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }



    public BaseResponse(int code, T data) {
        this.code = code;
        this.data = data;
    }

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data, String message, String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }

    public BaseResponse(ErrorCode error){
        this.code= error.getCode();;
        this.message= error.getMessage();
        this.description= error.getDescription();
    }

    public BaseResponse(ErrorCode error,T data){
        this.data=data;
        this.code= error.getCode();;
        this.message= error.getMessage();
        this.description= error.getDescription();
    }
}
