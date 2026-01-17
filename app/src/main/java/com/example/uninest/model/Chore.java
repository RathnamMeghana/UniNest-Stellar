package com.example.uninest.model;

public class Chore {

    private String id;
    private String taskName;
    private String room;
    private String houseCode;
    private int difficultyScore;
    private int estDurationMin;
    private int frequencyPerWeek;
    private String assignedTo; // roommate ID
    private Object createdAt;

    public String getId(){return id;}
    public void setId(String id){ this.id = id; }

    public String getTaskName(){ return taskName;}
    public void setTaskName(String taskName){ this.taskName = taskName; }

    public String getHouseCode(){ return houseCode;}
    public void setHouseCode(String houseCode){ this.houseCode = houseCode; }

    public String getRoom(){return room;}
    public void setRoom(String room){this.room = room;}

    public int getDifficultyScore(){ return difficultyScore;}
    public void setDifficultyScore(int difficultyScore){
        this.difficultyScore = difficultyScore;
    }
    public int getEstDurationMin(){return estDurationMin;}
    public void setEstDurationMin(int estDurationMin){
        this.estDurationMin = estDurationMin;
    }

    public int getFrequencyPerWeek(){return frequencyPerWeek;}
    public void setFrequencyPerWeek(int frequencyPerWeek){
        this.frequencyPerWeek = frequencyPerWeek;
    }

    public String getAssignedTo(){
        return assignedTo;
    }
    public void setAssignedTo(String assignedTo){
        this.assignedTo = assignedTo;
    }










}
