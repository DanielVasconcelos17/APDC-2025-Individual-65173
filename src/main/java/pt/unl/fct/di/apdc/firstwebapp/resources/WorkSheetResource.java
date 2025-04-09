package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.google.cloud.datastore.*;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.gson.Gson;
import com.google.cloud.datastore.Transaction;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.firstwebapp.authentication.TokenValidator;
import pt.unl.fct.di.apdc.firstwebapp.enums.Role;
import pt.unl.fct.di.apdc.firstwebapp.enums.WorkSheetState;
import pt.unl.fct.di.apdc.firstwebapp.response.TokenValidationResult;
import pt.unl.fct.di.apdc.firstwebapp.util.WorkSheetData;

@Path("/worksheet")
@Produces(MediaType.APPLICATION_JSON)
public class WorkSheetResource {

    public static final String WORKSHEET_REFERENCE = "worksheet_reference";
    public static final String WORKSHEET_DESCRIPTION = "worksheet_description";
    public static final String WORKSHEET_TARGET_TYPE = "worksheet_target_type";
    public static final String WORKSHEET_ADJUDICATION_STATE = "worksheet_adjudication_state";

    public static final String WORKSHEET_ADJUDICATION_DATE = "worksheet_adjudication_date";
    public static final String WORKSHEET_START_DATE = "worksheet_start_date";
    public static final String WORKSHEET_END_DATE = "worksheet_end_date";
    public static final String WORKSHEET_PARTNER_ACC = "worksheet_partner_account";
    public static final String WORKSHEET_COMPANY_NAME = "worksheet_company_name";
    public static final String WORKSHEET_COMPANY_NIF = "worksheet_company_nif";
    public static final String WORKSHEET_WORK_STATE = "worksheet_work_state";
    public static final String WORKSHEET_OBSERVATIONS = "worksheet_observations";


    private static final String TOKEN_ROLE = "token_role";
    private static final String TOKEN_USERNAME = "token_username";


    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory worksheetKeyFactory = datastore.newKeyFactory().setKind("WorkSheet");
    private final Gson g = new Gson();

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createWorkSheet(WorkSheetData data) {

        Transaction txn = datastore.newTransaction();
        try {
            // Validação habitual do token de autenticaçao
            TokenValidationResult validation =
                    TokenValidator.validateToken(txn, datastore, data.author);
            if (validation.errorResponse != null)
                return validation.errorResponse;

            String tokenRole = validation.tokenEntity.getString(TOKEN_ROLE);
            if (!tokenRole.equals(Role.BACKOFFICE.getType()))
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(g.toJson("Only BACKOFFICE users can create worksheets."))
                        .build();
            if (data.reference == null || data.description == null
                    || data.targetType == null || data.adjudicationState == null)
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(g.toJson("Missing mandatory attributes:" +
                                "reference, description, targetType or adjudicationState."))
                        .build();

            Key worksheetKey = worksheetKeyFactory.newKey(data.reference);
            //Cria a folha de obra (worksheet) na datastore
            Entity.Builder worksheetBuilder = Entity.newBuilder(worksheetKey)
                    .set(WORKSHEET_REFERENCE, data.reference)
                    .set(WORKSHEET_DESCRIPTION, data.description)
                    .set(WORKSHEET_TARGET_TYPE, data.targetType)
                    .set(WORKSHEET_ADJUDICATION_STATE, data.adjudicationState);

            // Atributos adicionais
            if (data.adjudicationDate != null) worksheetBuilder.set(WORKSHEET_ADJUDICATION_DATE, data.adjudicationDate);
            if (data.startDate != null) worksheetBuilder.set(WORKSHEET_START_DATE, data.startDate);
            if (data.endDate != null) worksheetBuilder.set(WORKSHEET_END_DATE, data.endDate);
            if (data.partnerAccount != null) worksheetBuilder.set(WORKSHEET_PARTNER_ACC, data.partnerAccount);
            if (data.companyName != null) worksheetBuilder.set(WORKSHEET_COMPANY_NAME, data.companyName);
            if (data.companyNIF != null) worksheetBuilder.set(WORKSHEET_COMPANY_NIF, data.companyNIF);
            if (data.workState != null) worksheetBuilder.set(WORKSHEET_WORK_STATE, data.workState);
            if (data.observations != null) worksheetBuilder.set(WORKSHEET_OBSERVATIONS, data.observations);

            txn.put(worksheetBuilder.build());
            txn.commit();
            WorkSheetData view = new WorkSheetData(worksheetBuilder.build());
            return Response.ok(g.toJson(view))
                    .build();
        } catch (Exception e) {
            txn.rollback();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(g.toJson("Error creating worksheet: " + e.getMessage()))
                    .build();
        } finally {
            if (txn.isActive())
                txn.rollback();
        }
    }

    @POST
    @Path("/changeAttributes")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeAttributesWorkSheet(WorkSheetData data) {

        Transaction txn = datastore.newTransaction();
        try {
            TokenValidationResult validation = TokenValidator.validateToken(txn, datastore, data.author);
            if (validation.errorResponse != null) {
                return validation.errorResponse;
            }

            Entity tokenEntity = validation.tokenEntity;
            String username = tokenEntity.getString(TOKEN_USERNAME);
            String role = tokenEntity.getString(TOKEN_ROLE);

            if (!role.equals(Role.PARTNER.getType()))
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(g.toJson("Only PARTNER users can update worksheet state."))
                        .build();


            // Buscar a folha de obra no datastore
            Key worksheetKey = worksheetKeyFactory.newKey(data.reference);
            Entity worksheet = txn.get(worksheetKey);

            if (worksheet == null)
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(g.toJson("Worksheet not found."))
                        .build();


            // Verificar se a obra foi adjudicada ao PARTNER em questao
            String partnerAccount = worksheet.getString(WORKSHEET_PARTNER_ACC);
            if (!partnerAccount.equals(username)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(g.toJson("This worksheet is not adjudicated to your account."))
                        .build();
            }

            //Validar o novo estado
            if (WorkSheetState.fromString(data.workState) == null)
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(g.toJson("Invalid state (NOT_STARTED, IN_PROGRESS, or COMPLETED)."))
                        .build();

            // Atualizar apenas o campo WORKSHEET_WORK_STATE
            Entity updatedWorksheet = Entity.newBuilder(worksheet)
                    .set(WORKSHEET_WORK_STATE, data.workState)
                    .build();
            txn.put(updatedWorksheet);
            txn.commit();
            WorkSheetData view = new WorkSheetData(updatedWorksheet);

            return Response.ok(g.toJson(view)).build();
        } catch (Exception e) {
            txn.rollback();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(g.toJson("Error creating worksheet: " + e.getMessage()))
                    .build();
        } finally {
            if (txn.isActive())
                txn.rollback();
        }
    }

}
