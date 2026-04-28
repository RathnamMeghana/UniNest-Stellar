package com.example.uninest.model;

public class Room {
    private String houseCode;
    private String type;
    private String label;


    public String getHouseCode(){
        return houseCode;
    }
    public void setHouseCode(String houseCode){
        this.houseCode = houseCode;
    }

    public String getType(){
        return type;
    }

    public void setType(String type){
        this.type = type;
    }

    public String getLabel(){
        return label;
    }
    public void setLabel(String label){
        this.label = label;
    }
}
