package com.yurii.zhuravlov.gateway.jwt;

import com.yurii.zhuravlov.jwt.AbstractJwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService extends AbstractJwtService {

    @Value("${jwt.secret}")
    private String secretKey;


    @Override
    protected String getSecretKey() {
        return secretKey;
    }

}
