package com.houcinee.bankapp.service;

import com.houcinee.bankapp.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.currentTimeMillis;

@Service
public class JwtService {
	@Value("${security.jwt.secret-key}")
	private String secretKey;

	@Value("${security.jwt.expiration}")
	private Long jwtExpiration;


	public String generateToken(AppUser appUser) {
		HashMap<String, Object> claims = new HashMap<>();
		List<String> roles = appUser.getAuthorities()
		                            .stream()
		                            .map(GrantedAuthority::getAuthority)
		                            .toList();
		claims.put("roles", roles);
		claims.put("userId" , appUser.getId()) ;
		return generateToken(claims, appUser);
	}

	private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
		return Jwts.builder()
		           .claims(extraClaims)
		           .subject(userDetails.getUsername())
		           .issuedAt(new Date(currentTimeMillis()))
		           .expiration(new Date(currentTimeMillis() + jwtExpiration))
		           .signWith(getSignInKey())
		           .compact();
	}


	public Claims extractAllClaims(String token) {
		return Jwts.parser()
		           .verifyWith(getSignInKey())
		           .build()
		           .parseSignedClaims(token)
		           .getPayload();
	}

	private SecretKey getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}


}
