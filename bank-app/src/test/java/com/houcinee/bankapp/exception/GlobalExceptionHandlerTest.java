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

@WebMvcTest(controllers = DummyController.class)
public class GlobalExceptionHandlerTest extends BaseWebMvcTest {

    @Test
    public void givenResourceNotFoundException_whenHandle_thenReturns404() throws Exception {
        mockMvc.perform(get("/test/notfound"))
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
        mockMvc.perform(get("/test/illegalargument"))
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
        mockMvc.perform(get("/test/globalException"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected server error occurred."))
                .andExpect(jsonPath("$.path").value("/test/globalException"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void givenValidationException_whenHandle_thenReturns400() throws Exception{
       mockMvc.perform(post("/test/validationException") )
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.status").value(400))
               .andExpect(jsonPath("$.error").value("Bad Request"))
               .andExpect(jsonPath("$.path").value("/test/validationException"))
               .andExpect(jsonPath("$.correlationId").exists())
               .andExpect(jsonPath("$.timestamp").exists());
    }
}
@RestController
class DummyController{

    @GetMapping("/test/notfound")
    public void throwNotFound(){
        throw new ResourceNotFoundException("Resource Not found") ;
    }
    @GetMapping("/test/illegalargument")
    public void throwIllegalArgument(){
        throw new IllegalArgumentException("Illegal Argument") ;
    }
    @GetMapping("/test/globalException")
    public void throwRunTimeException(){
        throw new RuntimeException("Run time Exception") ;
    }
    @PostMapping("/test/validationException")
    public void throwValidationException(@Valid TestDto testDto){}
}
record TestDto(@NotNull String name){}
