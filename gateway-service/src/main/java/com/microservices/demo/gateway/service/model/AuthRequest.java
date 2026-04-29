package com.microservices.demo.gateway.service.model;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}
