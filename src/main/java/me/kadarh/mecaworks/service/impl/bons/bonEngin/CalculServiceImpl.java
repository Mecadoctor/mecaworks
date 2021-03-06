package me.kadarh.mecaworks.service.impl.bons.bonEngin;

import lombok.extern.slf4j.Slf4j;
import me.kadarh.mecaworks.domain.bons.BonEngin;
import me.kadarh.mecaworks.domain.others.TypeCompteur;
import me.kadarh.mecaworks.repo.bons.BonEnginRepo;
import me.kadarh.mecaworks.repo.others.ChantierRepo;
import me.kadarh.mecaworks.repo.others.EmployeRepo;
import me.kadarh.mecaworks.repo.others.EnginRepo;
import me.kadarh.mecaworks.service.exceptions.OperationFailedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kadarH
 */

@Service
@Slf4j
@Transactional
public class CalculServiceImpl {

    private final BonEnginRepo bonEnginRepo;
    private final EnginRepo enginRepo;
    private final ChantierRepo chantierRepo;
    private final EmployeRepo employeRepo;
    private PersistServiceImpl persistService;

    public CalculServiceImpl(BonEnginRepo bonEnginRepo, EnginRepo enginRepo, ChantierRepo chantierRepo, EmployeRepo employeRepo, PersistServiceImpl persistService) {
        this.bonEnginRepo = bonEnginRepo;
        this.enginRepo = enginRepo;
        this.chantierRepo = chantierRepo;
        this.employeRepo = employeRepo;
        this.persistService = persistService;
    }

    public BonEngin fillBon(BonEngin bon) {
        try {
            bon.setEngin(enginRepo.findById(bon.getEngin().getId()).get());
            bon.setChauffeur(employeRepo.findById(bon.getChauffeur().getId()).get());
            bon.setPompiste(employeRepo.findById(bon.getPompiste().getId()).get());
            bon.setChantierTravail(chantierRepo.findById(bon.getChantierTravail().getId()).get());
            bon.setChantierGazoil(chantierRepo.findById(bon.getChantierGazoil().getId()).get());
            calculCompteursAbsolu(bon, persistService.getLastBonEngin(bon.getEngin()));
            log.info("Compteur Absolu H = " + bon.getCompteurAbsoluH());
            log.info("Compteur Absolu Km = " + bon.getCompteurAbsoluKm());
            log.info("Bon has been filled correctly");

            return bon;
        } catch (Exception e) {
            throw new OperationFailedException("Operation echouée", e);
        }
    }

    public void calculCompteursAbsolu(BonEngin bonEngin, BonEngin lastBon) {
        log.info("--- > Calcul compteur Absolu");
        String typeCompteur = bonEngin.getEngin().getSousFamille().getTypeCompteur().name();
        BonEngin bonEngin1 = lastBon;
        if (typeCompteur.equals(TypeCompteur.H.name())) {
            if (bonEngin1 != null) {
                if (bonEngin.getCompteurHenPanne()) {
                    bonEngin.setCompteurH(bonEngin1.getCompteurH());
                    bonEngin.setCompteurAbsoluH(bonEngin1.getCompteurAbsoluH());
                }
                else
                    bonEngin.setCompteurAbsoluH(bonEngin1.getCompteurAbsoluH() + bonEngin.getCompteurH() - bonEngin1.getCompteurH());
                setCompteurAbsoluH_ifCmpBonInfCmptLastBon(bonEngin, bonEngin1);
            }
            else
                bonEngin.setCompteurAbsoluH(bonEngin.getCompteurH());
        } else if (typeCompteur.equals(TypeCompteur.KM.name())) {
            if (bonEngin1 != null) {
                if (bonEngin.getCompteurKmenPanne()) {
                    bonEngin.setCompteurKm(bonEngin1.getCompteurKm());
                    bonEngin.setCompteurAbsoluKm(bonEngin1.getCompteurAbsoluKm());
                }
                else
                    bonEngin.setCompteurAbsoluKm(bonEngin1.getCompteurAbsoluKm() + bonEngin.getCompteurKm() - bonEngin1.getCompteurKm());
                setCompteurAbsoluKm_ifCmpBonInfCmptLastBon(bonEngin, bonEngin1);
            }
            else
                bonEngin.setCompteurAbsoluKm(bonEngin.getCompteurKm());
        } else if (typeCompteur.equals(TypeCompteur.KM_H.name())) {
            if (bonEngin1 != null) {
                if (bonEngin.getCompteurHenPanne()) {
                    bonEngin.setCompteurH(bonEngin1.getCompteurH());
                    bonEngin.setCompteurAbsoluH(bonEngin1.getCompteurAbsoluH());
                }
                else
                    bonEngin.setCompteurAbsoluH(bonEngin1.getCompteurAbsoluH() + bonEngin.getCompteurH() - bonEngin1.getCompteurH());
                setCompteurAbsoluH_ifCmpBonInfCmptLastBon(bonEngin, bonEngin1);

                if (bonEngin.getCompteurKmenPanne()) {
                    bonEngin.setCompteurKm(bonEngin1.getCompteurKm());
                    bonEngin.setCompteurAbsoluKm(bonEngin1.getCompteurAbsoluKm());
                }
                else
                    bonEngin.setCompteurAbsoluKm(bonEngin1.getCompteurAbsoluKm() + bonEngin.getCompteurKm() - bonEngin1.getCompteurKm());
                setCompteurAbsoluKm_ifCmpBonInfCmptLastBon(bonEngin, bonEngin1);
            } else {
                bonEngin.setCompteurAbsoluH(bonEngin.getCompteurH());
                bonEngin.setCompteurAbsoluKm(bonEngin.getCompteurKm());
            }
        }
        log.info("--- > Calcul compteur AbsoluKm = " + bonEngin.getCompteurKm());
        log.info("--- > Calcul compteur AbsoluH = " + bonEngin.getCompteurH());
    }

    private void setCompteurAbsoluH_ifCmpBonInfCmptLastBon(BonEngin bonEngin, BonEngin bonEngin1) {
        if (bonEngin.getCompteurH() < bonEngin1.getCompteurH()) {
            if (bonEngin.getCompteurHenPanne()) {
                bonEngin.setCompteurAbsoluH(bonEngin1.getCompteurAbsoluH());
                bonEngin.setCompteurH(bonEngin1.getCompteurH());
            } else
                bonEngin.setCompteurAbsoluH(bonEngin1.getCompteurAbsoluH() + bonEngin.getCompteurH());
        }
    }

    private void setCompteurAbsoluKm_ifCmpBonInfCmptLastBon(BonEngin bonEngin, BonEngin bonEngin1) {
        if (bonEngin.getCompteurKm() < bonEngin1.getCompteurKm()) {
            if (bonEngin.getCompteurKmenPanne()) {
                bonEngin.setCompteurAbsoluKm(bonEngin1.getCompteurAbsoluKm());
                bonEngin.setCompteurKm(bonEngin1.getCompteurKm());
            } else
                bonEngin.setCompteurAbsoluKm(bonEngin1.getCompteurAbsoluKm() + bonEngin.getCompteurKm());
        }
    }

    public BonEngin calculConsommation(BonEngin bonEngin) {
        //get last bon Engin [ BX ] with cmpEnpanne=non && plein =oui
        //get list of bons between BX and the current bon
        //SOM_Q = calculate lmejmou3 dial les quantite ,
        // AA = absolu current bon - absolu BX
        //calculate consommatio = SOM_Q/AA ( l7ala dial km : SOM_Q*100/AA
        TypeCompteur typeCompteur = bonEngin.getEngin().getSousFamille().getTypeCompteur();
        BonEngin lastBon, lastBon2;
        long som_Q = 0;
        long som_Q_2 = 0;
        List<BonEngin> list = new ArrayList<>();
        if (typeCompteur.equals(TypeCompteur.H)) {
            lastBon = bonEnginRepo.findLastBonEnginH_toConsommation(bonEngin.getEngin().getId());
            if (lastBon != null) {
                list = bonEnginRepo.findAllBetweenLastBonAndCurrentBon_H(bonEngin.getEngin().getId(), lastBon.getCompteurAbsoluH());
                list.remove(lastBon);
                for (BonEngin b : list)
                    som_Q += b.getQuantite();
                som_Q += bonEngin.getQuantite();
                if (bonEngin.getCompteurAbsoluH() > lastBon.getCompteurAbsoluH())
                    bonEngin.setConsommationH((float) som_Q / (bonEngin.getCompteurAbsoluH() - lastBon.getCompteurAbsoluH()));
                else bonEngin.setConsommationH(0f);
            }
            if (bonEngin.getCompteurHenPanne() || (!list.isEmpty() && list.get(list.size() - 1).getCompteurHenPanne()))
                bonEngin.setConsommationH(0f);
        }
        if (typeCompteur.equals(TypeCompteur.KM)) {
            lastBon = bonEnginRepo.findLastBonEnginKm_toConsommation(bonEngin.getEngin().getId());
            if (lastBon != null) {
                list = bonEnginRepo.findAllBetweenLastBonAndCurrentBon_Km(bonEngin.getEngin().getId(), lastBon.getCompteurAbsoluKm());
                list.remove(lastBon);
                for (BonEngin b : list)
                    som_Q += b.getQuantite();
                som_Q += bonEngin.getQuantite();
                if (bonEngin.getCompteurAbsoluKm() > lastBon.getCompteurAbsoluKm())
                    bonEngin.setConsommationKm((float) som_Q * 100 / (bonEngin.getCompteurAbsoluKm() - lastBon.getCompteurAbsoluKm()));
                else bonEngin.setConsommationKm(0f);
            }
            if (bonEngin.getCompteurKmenPanne() || (!list.isEmpty() && list.get(list.size() - 1).getCompteurKmenPanne()))
                bonEngin.setConsommationKm(0f);
        }
        if (typeCompteur.equals(TypeCompteur.KM_H)) {
            lastBon = bonEnginRepo.findLastBonEnginKm_toConsommation(bonEngin.getEngin().getId());
            lastBon2 = bonEnginRepo.findLastBonEnginH_toConsommation(bonEngin.getEngin().getId());
            if (lastBon != null) {
                list = bonEnginRepo.findAllBetweenLastBonAndCurrentBon_Km(bonEngin.getEngin().getId(), lastBon.getCompteurAbsoluKm());
                list.remove(lastBon);
                for (BonEngin b : list) {
                    if (b.getQuantite() != null)
                        som_Q += b.getQuantite();
                }
                som_Q += bonEngin.getQuantite();
                bonEngin.setConsommationKm((float) som_Q * 100 / (bonEngin.getCompteurAbsoluKm() - lastBon.getCompteurAbsoluKm()));
            }
            if (bonEngin.getCompteurHenPanne() || (!list.isEmpty() && list.get(list.size() - 1).getCompteurHenPanne()))
                bonEngin.setConsommationH(0f);
            if (lastBon2 != null) {
                list = bonEnginRepo.findAllBetweenLastBonAndCurrentBon_H(bonEngin.getEngin().getId(), lastBon2.getCompteurAbsoluH());
                list.remove(lastBon);
                for (BonEngin b : list) {
                    if (b.getQuantite() != null)
                        som_Q_2 += b.getQuantite();
                }
                som_Q_2 += bonEngin.getQuantite();
                bonEngin.setConsommationH((float) som_Q_2 / (bonEngin.getCompteurAbsoluH() - lastBon2.getCompteurAbsoluH()));
            }
            if (bonEngin.getCompteurKmenPanne() || (!list.isEmpty() && list.get(list.size() - 1).getCompteurKmenPanne()))
                bonEngin.setConsommationKm(0f);
        }
        return bonEngin;
    }

}
