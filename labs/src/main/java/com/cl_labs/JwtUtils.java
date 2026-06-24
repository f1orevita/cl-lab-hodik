package com.cl_labs;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.util.Date;

public class JwtUtils {
    private static final String SECRET = "MySuperSecretKeyForJWT"; // У реальному житті це зберігають у змінних середовища
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET);
    private static final String ISSUER = "StoreServer";

    public static String generateToken(String username) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(username)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
                .sign(ALGORITHM);
    }

    public static boolean verifyToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(ALGORITHM)
                    .withIssuer(ISSUER)
                    .build();
            verifier.verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}