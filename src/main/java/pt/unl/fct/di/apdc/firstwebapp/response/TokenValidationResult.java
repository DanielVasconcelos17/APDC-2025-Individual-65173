package pt.unl.fct.di.apdc.firstwebapp.response;

import com.google.cloud.datastore.Entity;
import jakarta.ws.rs.core.Response;

public class TokenValidationResult {
    public Response errorResponse;
    public Entity tokenEntity;

    public TokenValidationResult(Response errorResponse, Entity tokenEntity) {
        this.errorResponse = errorResponse;
        this.tokenEntity = tokenEntity;
    }
}
