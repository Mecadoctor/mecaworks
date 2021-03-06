package me.kadarh.mecaworks.service.impl.bons.bonEngin;

import lombok.extern.slf4j.Slf4j;
import me.kadarh.mecaworks.domain.alertes.Alerte;
import me.kadarh.mecaworks.domain.alertes.TypeAlerte;
import me.kadarh.mecaworks.domain.bons.BonEngin;
import me.kadarh.mecaworks.domain.others.Engin;
import me.kadarh.mecaworks.domain.others.SousFamille;
import me.kadarh.mecaworks.domain.others.Stock;
import me.kadarh.mecaworks.domain.others.TypeCompteur;
import me.kadarh.mecaworks.repo.bons.BonEnginRepo;
import me.kadarh.mecaworks.service.AlerteService;
import me.kadarh.mecaworks.service.StockService;
import me.kadarh.mecaworks.service.exceptions.OperationFailedException;
import me.kadarh.mecaworks.service.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;

/**
 * @author kadarH
 */

@Service
@Slf4j
@Transactional
public class PersistServiceImpl {

    private final AlerteService alerteService;
    private final StockService stockService;
    private final BonEnginRepo bonEnginRepo;

    public PersistServiceImpl(AlerteService alerteService, StockService stockService, BonEnginRepo bonEnginRepo) {
        this.alerteService = alerteService;
        this.stockService = stockService;
        this.bonEnginRepo = bonEnginRepo;
    }

    public void insertAlertes(BonEngin bonEngin) {
        BonEngin lastBonEngin = getLastBonEngin(bonEngin.getEngin());
        boolean okey = false;
        if (lastBonEngin != null) okey = true;
        // Verify if chauffeur is changed.
        if (okey && lastBonEngin.getChauffeur() != bonEngin.getChauffeur())
            insertAlerte(bonEngin, "Chauffeur changé", TypeAlerte.CHAUFFEUR_CHANGED);
        // Verify if compteur is en panne , and if no verify if compteur last bon was en panne
        if (whichCompteurIsDown(bonEngin) == 1)
            insertAlerte(bonEngin, "Compteur H En Panne", TypeAlerte.COMPTEUR_H_EN_PANNE);
        if (whichCompteurIsDown(bonEngin) == 2 && okey)
            insertAlerte(bonEngin, "Compteur Km En Panne", TypeAlerte.COMPTEUR_KM_EN_PANNE);
        if (whichCompteurIsDown(bonEngin) == 3) {
            insertAlerte(bonEngin, "Compteur H En Panne", TypeAlerte.COMPTEUR_H_EN_PANNE);
            insertAlerte(bonEngin, "Compteur Km En Panne", TypeAlerte.COMPTEUR_KM_EN_PANNE);
        }
        //verify quantité
        if (bonEngin.getQuantite() > bonEngin.getEngin().getSousFamille().getCapaciteReservoir())
            insertAlerte(bonEngin, "Quantité de gazoil depasse la capacité de reservoir", TypeAlerte.QUANTITE_MORE_THAN_RESERVOIR);
        // FIXME: 20/05/18 : add alerte ( compteur reparé ) 
    }

    public void insertAlerte(BonEngin bonEngin, String msg, TypeAlerte type) {
        Alerte alerte = new Alerte();
        alerte.setDate(LocalDate.now());
        alerte.setIdBon(bonEngin.getId());
        alerte.setMessage(msg);
        alerte.setEtat(true);
        alerte.setTypeAlerte(type);
        alerteService.add(alerte);
    }

    public void insertStock(BonEngin bonEngin) {
        Stock stock = new Stock();
        stock.setChantier(bonEngin.getChantierTravail());
        stock.setDate(bonEngin.getDate());
        stock.setSortieE(bonEngin.getQuantite());
        if (stockService.getLastStock() != null)
            stock.setStockC(stockService.getLastStock().getStockC() - bonEngin.getQuantite());
        else stock.setStockC(bonEngin.getChantierGazoil().getStock());
        stockService.add(stock);
    }


    public int whichCompteurIsDown(BonEngin bonEngin) {
        boolean cmpHenPanne = bonEngin.getCompteurHenPanne();
        boolean cmpKmenPanne = bonEngin.getCompteurKmenPanne();
        if (cmpHenPanne && cmpKmenPanne)
            return 3;
        else if (cmpKmenPanne)
            return 2;
        else if (cmpHenPanne)
            return 1;
        else
            return 0;
    }

    public BonEngin getLastBonEngin(Engin engin) {
        try {
            return bonEnginRepo.findLastBonEngin(engin.getId());
        } catch (NoSuchElementException e) {
            log.info("Problem , cannot find last BonEngin with id = :" + engin.getId());
            throw new ResourceNotFoundException("BonEngin introuvable, c'est le premier bon de cette engin", e);
        } catch (Exception e) {
            log.info("Problem , cannot find last BonEngin with id = :" + engin.getId());
            throw new OperationFailedException("Operation echouée", e);
        }
    }

    public void insertAlerteConsommation(BonEngin bonEngin) {
        SousFamille sousFamille = bonEngin.getEngin().getSousFamille();
        if (sousFamille == null) return;
        String typeCompteur = sousFamille.getTypeCompteur().name();
        String msgH = "La consommation H est Annormale";
        String msgKm = "La consommation Km est Annormale";
        if (typeCompteur.equals(TypeCompteur.H.name())) {
            if (bonEngin.getConsommationH() > sousFamille.getConsommationHMax())
                this.insertAlerte(bonEngin, msgH, TypeAlerte.CONSOMMATION_H_ANNORMALE);
        }
        if (typeCompteur.equals(TypeCompteur.KM.name())) {
            if (bonEngin.getConsommationKm() > sousFamille.getConsommationKmMax())
                this.insertAlerte(bonEngin, msgKm, TypeAlerte.CONSOMMATION_KM_ANNORMALE);
        }
        if (typeCompteur.equals(TypeCompteur.KM_H.name())) {
            if (bonEngin.getConsommationH() > sousFamille.getConsommationHMax())
                this.insertAlerte(bonEngin, msgH, TypeAlerte.CONSOMMATION_H_ANNORMALE);
            if (bonEngin.getConsommationKm() > sousFamille.getConsommationKmMax())
                this.insertAlerte(bonEngin, msgKm, TypeAlerte.CONSOMMATION_KM_ANNORMALE);
        }
    }
}
