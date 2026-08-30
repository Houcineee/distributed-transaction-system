package com.houcinee.bankapp.filter;

import com.houcinee.bankapp.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {

		final String authHeader = request.getHeader("Authorization");
		final String jwt;

		if(authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		jwt = authHeader.substring(7);

		if(SecurityContextHolder.getContext().getAuthentication() == null){

			try {
				var claims = jwtService.extractAllClaims(jwt) ;
				log.debug("Authenticated user {} via JWT", claims.getSubject());

				List<String> roles = (List<String>) claims.get("roles");
				List<SimpleGrantedAuthority> authorities = (roles != null ? roles : Collections.<String>emptyList())
						.stream()
						.map(SimpleGrantedAuthority::new)
						.toList();

				var authToken = new UsernamePasswordAuthenticationToken(
						claims.getSubject(),
						null,
						authorities
				);
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);
			} catch (Exception e) {
				log.warn("Invalid JWT token detected: {}", e.getMessage());
			}
		}

		filterChain.doFilter(request, response);
	}
}
