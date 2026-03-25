package com.ipiecoles.java.java350.service;

import com.ipiecoles.java.java350.exception.EmployeException;
import com.ipiecoles.java.java350.model.Employe;
import com.ipiecoles.java.java350.model.Entreprise;
import com.ipiecoles.java.java350.model.NiveauEtude;
import com.ipiecoles.java.java350.model.Poste;
import com.ipiecoles.java.java350.repository.EmployeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityExistsException;
import java.time.LocalDate;

@Service
public class EmployeService {

    @Autowired
    private EmployeRepository employeRepository;

    public void embaucheEmploye(String nom, String prenom, Poste poste, NiveauEtude niveauEtude, Double tempsPartiel) throws EmployeException, EntityExistsException {

        String typeEmploye = poste.name().substring(0,1);

        String lastMatricule = employeRepository.findLastMatricule();
        if(lastMatricule == null){
            lastMatricule = Entreprise.MATRICULE_INITIAL;
        }

        Integer numeroMatricule = Integer.parseInt(lastMatricule) + 1;

        if(numeroMatricule >= 100000){
            throw new EmployeException("Limite des 100000 matricules atteinte !");
        }

        String matricule = "00000" + numeroMatricule;
        matricule = typeEmploye + matricule.substring(matricule.length() - 5);

        if(employeRepository.findByMatricule(matricule) != null){
            throw new EntityExistsException("L'employé de matricule " + matricule + " existe déjà en BDD");
        }

        Double salaire = Entreprise.COEFF_SALAIRE_ETUDES.get(niveauEtude) * Entreprise.SALAIRE_BASE;

        if(tempsPartiel != null){
            salaire = salaire * tempsPartiel;
        }

        salaire = Math.round(salaire * 100d) / 100d;

        Employe employe = new Employe(nom, prenom, matricule, LocalDate.now(), salaire, Entreprise.PERFORMANCE_BASE, tempsPartiel);

        employeRepository.save(employe);
    }

    public void calculPerformanceCommercial(String matricule, Long caTraite, Long objectifCa) throws EmployeException {

        // ✅ Vérifications externalisées (plus de duplication)
        verifierCa(caTraite);
        verifierObjectif(objectifCa);
        verifierMatricule(matricule);

        // Recherche employé
        Employe employe = employeRepository.findByMatricule(matricule);
        if(employe == null){
            throw new EmployeException("Le matricule " + matricule + " n'existe pas !");
        }

        Integer performance = Entreprise.PERFORMANCE_BASE;

        double ratio = caTraite / (double) objectifCa;

        if(ratio >= 0.8 && ratio < 0.95){
            performance = Math.max(Entreprise.PERFORMANCE_BASE, employe.getPerformance() - 2);
        }
        else if(ratio < 1.05){
            performance = Math.max(Entreprise.PERFORMANCE_BASE, employe.getPerformance());
        }
        else if(ratio <= 1.2){
            performance = employe.getPerformance() + 1;
        }
        else{
            performance = employe.getPerformance() + 4;
        }

        Double performanceMoyenne = employeRepository.avgPerformanceWhereMatriculeStartsWith("C");
        if(performanceMoyenne != null && performance > performanceMoyenne){
            performance++;
        }

        employe.setPerformance(performance);
        employeRepository.save(employe);
    }

    // ✅ Méthodes pour réduire la complexité (Sonar OK)

    private void verifierCa(Long caTraite) throws EmployeException {
        if (caTraite == null || caTraite < 0) {
            throw new EmployeException("Le chiffre d'affaire traité ne peut être négatif ou null !");
        }
    }

    private void verifierObjectif(Long objectifCa) throws EmployeException {
        if (objectifCa == null || objectifCa < 0) {
            throw new EmployeException("L'objectif de chiffre d'affaire ne peut être négatif ou null !");
        }
    }

    private void verifierMatricule(String matricule) throws EmployeException {
        if (matricule == null || !matricule.startsWith("C")) {
            throw new EmployeException("Le matricule ne peut être null et doit commencer par un C !");
        }
    }
}
