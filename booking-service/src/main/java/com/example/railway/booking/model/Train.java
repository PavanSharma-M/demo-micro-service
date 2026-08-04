package com.example.railway.booking.model;

/** Mirrors Train.TrainType from train-service for Feign DTO deserialization */
public class Train {
    public enum TrainType {
        EXPRESS, SUPERFAST, LOCAL, RAJDHANI, SHATABDI
    }
}
