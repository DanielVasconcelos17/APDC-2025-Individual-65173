package pt.unl.fct.di.apdc.firstwebapp.util;

import com.google.cloud.datastore.Entity;
import pt.unl.fct.di.apdc.firstwebapp.resources.WorkSheetResource;

public class WorkSheetData {

    // Criador/Modificador da Worksheet
    public String author;

    // Atributos obrigatórios
    public String reference;       // Referência da obra
    public String description;
    public String targetType;      // Tipo de alvo Propriedade pública/privada
    public String adjudicationState; // Estado de adjudicação adjudicado/não adjudicado

    // Atributos opcionais
    public String adjudicationDate;
    public String startDate;
    public String endDate;
    public String partnerAccount;
    public String companyName;
    public String companyNIF;
    public String workState;
    public String observations;

    public WorkSheetData() {
    }

    public WorkSheetData(Entity worksheet) {
        this.reference = assignString(worksheet, WorkSheetResource.WORKSHEET_REFERENCE);
        this.description = assignString(worksheet, WorkSheetResource.WORKSHEET_DESCRIPTION);
        this.targetType = assignString(worksheet, WorkSheetResource.WORKSHEET_TARGET_TYPE);
        this.adjudicationState = assignString(worksheet, WorkSheetResource.WORKSHEET_ADJUDICATION_STATE);
        this.adjudicationDate = assignString(worksheet, WorkSheetResource.WORKSHEET_ADJUDICATION_DATE); // Corrigido!
        this.startDate = assignString(worksheet, WorkSheetResource.WORKSHEET_START_DATE);
        this.endDate = assignString(worksheet, WorkSheetResource.WORKSHEET_END_DATE);
        this.partnerAccount = assignString(worksheet, WorkSheetResource.WORKSHEET_PARTNER_ACC);
        this.companyName = assignString(worksheet, WorkSheetResource.WORKSHEET_COMPANY_NAME);
        this.companyNIF = assignString(worksheet, WorkSheetResource.WORKSHEET_COMPANY_NIF);
        this.workState = assignString(worksheet, WorkSheetResource.WORKSHEET_WORK_STATE);
        this.observations = assignString(worksheet, WorkSheetResource.WORKSHEET_OBSERVATIONS);
    }

    private String assignString(Entity worksheet, String key) {
        return worksheet.contains(key) ? worksheet.getString(key) : null;
    }

}
