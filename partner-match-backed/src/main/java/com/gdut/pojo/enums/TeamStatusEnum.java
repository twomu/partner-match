package com.gdut.pojo.enums;


import io.swagger.models.auth.In;

public enum TeamStatusEnum{
    PUBLIC(0,"公开"),
    PRIVATE(1,"私密"),
    SECRET(2, "加密");
    private int value;
    private String description;


    TeamStatusEnum(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public static TeamStatusEnum getEnumByValue(Integer status){
        if(status==null) return null;
        TeamStatusEnum[] enums=TeamStatusEnum.values();
        for (TeamStatusEnum teamStatusEnum : enums) {
            if(teamStatusEnum.getValue()==status)return teamStatusEnum;
        }
        return null;
    }
    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
