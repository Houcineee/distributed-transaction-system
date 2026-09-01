package com.houcinee.bankapp.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.houcinee.bankapp.controller.BaseWebMvcTest;
import java.util.UUID;

@WebMvcTest(controllers = DummyController.class)
public class GlobalExceptionHandlerTest extends BaseWebMvcTest {

	@Test
	public void givenResourceNotFoundException_whenHandle_thenReturns404() throws Exception {
		mockMvc.perform(get("/test/notfound").with(mockAuth(UUID.randomUUID(), "CUSTOMER")))
		       .andExpect(status().isNotFound())
		       .andExpect(jsonPath("$.status").value(404))
		       .andExpect(jsonPath("$.error").value("Not Found"))
		       .andExpect(jsonPath("$.message").value("Resource Not found"))
		       .andExpect(jsonPath("$.path").value("/test/notfound"))
		       .andExpect(jsonPath("$.correlationId").exists())
		       .andExpect(jsonPath("$.timestamp").exists());

	}

	@Test
	public void givenIllegalArgumentException_whenHandle_thenReturns400() throws Exception {
		mockMvc.perform(get("/test/illegalargument").with(mockAuth(UUID.randomUUID(), "CUSTOMER")))
		       .andExpect(status().isBadRequest())
		       .andExpect(jsonPath("$.status").value(400))
		       .andExpect(jsonPath("$.error").value("Bad Request"))
		       .andExpect(jsonPath("$.message").value("Illegal Argument"))
		       .andExpect(jsonPath("$.path").value("/test/illegalargument"))
		       .andExpect(jsonPath("$.correlationId").exists())
		       .andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	public void givenGenericException_whenHandle_thenReturns500() throws Exception {
		mockMvc.perform(get("/test/globalException").with(mockAuth(UUID.randomUUID(), "CUSTOMER")))
		       .andExpect(status().isInternalServerError())
		       .andExpect(jsonPath("$.status").value(500))
		       .andExpect(jsonPath("$.error").value("Internal Server Error"))
		       .andExpect(jsonPath("$.message").value("An unexpected server error occurred."))
		       .andExpect(jsonPath("$.path").value("/test/globalException"))
		       .andExpect(jsonPath("$.correlationId").exists())
		       .andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	public void givenValidationException_whenHandle_thenReturns400() throws Exception {
		mockMvc.perform(post("/test/validationException").with(mockAuth(UUID.randomUUID(), "CUSTOMER")))
		       .andExpect(status().isBadRequest())
		       .andExpect(jsonPath("$.status").value(400))
		       .andExpect(jsonPath("$.error").value("Bad Request"))
		       .andExpect(jsonPath("$.path").value("/test/validationException"))
		       .andExpect(jsonPath("$.correlationId").exists())
		       .andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	public void givenBadCredentialsException_whenHandle_thenReturns401() throws Exception {
		mockMvc.perform(get("/test/badCredentials").with(mockAuth(UUID.randomUUID(), "CUSTOMER")))
		       .andExpect(status().isUnauthorized())
		       .andExpect(jsonPath("$.status").value(401))
		       .andExpect(jsonPath("$.error").value("Unauthorized"))
		       .andExpect(jsonPath("$.message").value("Invalid email or password"))
		       .andExpect(jsonPath("$.path").value("/test/badCredentials"))
		       .andExpect(jsonPath("$.correlationId").exists())
		       .andExpect(jsonPath("$.timestamp").exists());
	}
	@Test
	public void givenDuplicateResourceException_whenHandle_thenReturns409() throws Exception {
		mockMvc.perform(get("/test/duplicateResource").with(mockAuth(UUID.randomUUID(), "CUSTOMER")))
		       .andExpect(status().isConflict())
		       .andExpect(jsonPath("$.status").value(409))
		       .andExpect(jsonPath("$.error").value("Conflict"))
		       .andExpect(jsonPath("$.message").value("Duplicate Resource"))
		       .andExpect(jsonPath("$.path").value("/test/duplicateResource"));
	}

	@Test
	public void givenTypeMismatch_whenHandle_thenReturns400() throws Exception {
		mockMvc.perform(get("/test/typeMismatch/not-a-uuid").with(mockAuth(UUID.randomUUID(), "CUSTOMER")))
		       .andExpect(status().isBadRequest())
		       .andExpect(jsonPath("$.status").value(400))
		       .andExpect(jsonPath("$.error").value("Bad Request"))
		       .andExpect(jsonPath("$.message").value("Invalid format for parameter: id"))
		       .andExpect(jsonPath("$.path").value("/test/typeMismatch/not-a-uuid"));
	}

	@Test
	public void givenMethodNotSupported_whenHandle_thenReturns405() throws Exception {
		mockMvc.perform(post("/test/notfound").with(mockAuth(UUID.randomUUID(), "CUSTOMER"))) // POST to a GET endpoint
		       .andExpect(status().isMethodNotAllowed())
		       .andExpect(jsonPath("$.status").value(405))
		       .andExpect(jsonPath("$.error").value("Method Not Allowed"));
	}

	@Test
	public void givenNoResourceFound_whenHandle_thenReturns404() throws Exception {
		mockMvc.perform(get("/test/completely-unknown-url").with(mockAuth(UUID.randomUUID(), "CUSTOMER")))
		       .andExpect(status().isNotFound())
		       .andExpect(jsonPath("$.status").value(404))
		       .andExpect(jsonPath("$.error").value("Not Found"));
	}

	@Test
	public void givenAuthorizationDeniedException_whenHandle_thenReturns403() throws Exception {
		mockMvc.perform(get("/test/authorizationDenied").with(mockAuth(UUID.randomUUID(), "CUSTOMER")))
		       .andExpect(status().isForbidden())
		       .andExpect(jsonPath("$.status").value(403))
		       .andExpect(jsonPath("$.error").value("Forbidden"))
		       .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
	}
}

@RestController
class DummyController {

	@GetMapping("/test/notfound")
	public void throwNotFound() {
		throw new ResourceNotFoundException("Resource Not found");
	}

	@GetMapping("/test/illegalargument")
	public void throwIllegalArgument() {
		throw new IllegalArgumentException("Illegal Argument");
	}

	@GetMapping("/test/globalException")
	public void throwRunTimeException() {
		throw new RuntimeException("Run time Exception");
	}

	@PostMapping("/test/validationException")
	public void throwValidationException(@Valid TestDto testDto) {
	}

	@GetMapping("/test/badCredentials")
	public void throwBadCredentials() {
		throw new org.springframework.security.authentication.BadCredentialsException("Bad credentials");
	}

	@GetMapping("/test/duplicateResource")
	public void throwDuplicateResource() {
		throw new DuplicateResourceException("Duplicate Resource");
	}

	@GetMapping("/test/typeMismatch/{id}")
	public void throwTypeMismatch(@org.springframework.web.bind.annotation.PathVariable UUID id) {
	}

	@GetMapping("/test/authorizationDenied")
	public void throwAuthorizationDenied() {
		throw new org.springframework.security.authorization.AuthorizationDeniedException("Denied", new org.springframework.security.authorization.AuthorizationDecision(false));
	}
}

record TestDto(@NotNull String name) {
}
