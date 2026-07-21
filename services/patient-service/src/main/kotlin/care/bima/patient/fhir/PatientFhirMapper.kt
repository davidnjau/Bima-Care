package care.bima.patient.fhir

import care.bima.patient.domain.Gender
import care.bima.patient.domain.Patient
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Identifier
import java.time.ZoneId
import java.util.Date
import org.hl7.fhir.r4.model.Patient as FhirPatient

object PatientFhirMapper {
    fun toFhir(patient: Patient): FhirPatient =
        FhirPatient().apply {
            id = patient.id.toString()
            addIdentifier(Identifier().setValue(patient.nationalId))
            addName(HumanName().addGiven(patient.firstName).setFamily(patient.lastName))
            addTelecom(ContactPoint().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(patient.phone))
            gender =
                when (patient.gender) {
                    Gender.MALE -> Enumerations.AdministrativeGender.MALE
                    Gender.FEMALE -> Enumerations.AdministrativeGender.FEMALE
                    Gender.OTHER -> Enumerations.AdministrativeGender.OTHER
                    Gender.UNKNOWN -> Enumerations.AdministrativeGender.UNKNOWN
                }
            birthDate = Date.from(patient.dob.atStartOfDay(ZoneId.systemDefault()).toInstant())
            active = patient.isActive
        }
}
