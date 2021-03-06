package me.kadarh.mecaworks.service.impl.bons;

import lombok.extern.slf4j.Slf4j;
import me.kadarh.mecaworks.domain.bons.BonFournisseur;
import me.kadarh.mecaworks.domain.others.Chantier;
import me.kadarh.mecaworks.domain.others.Fournisseur;
import me.kadarh.mecaworks.domain.others.Stock;
import me.kadarh.mecaworks.repo.bons.BonFournisseurRepo;
import me.kadarh.mecaworks.service.StockService;
import me.kadarh.mecaworks.service.bons.BonFournisseurService;
import me.kadarh.mecaworks.service.exceptions.OperationFailedException;
import me.kadarh.mecaworks.service.exceptions.ResourceNotFoundException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author kadarH
 */

@Service
@Transactional
@Slf4j
public class BonFournisseurServiceImpl implements BonFournisseurService {

    private BonFournisseurRepo bonFournisseurRepo;
    private StockService stockService;

    public BonFournisseurServiceImpl(BonFournisseurRepo bonFournisseurRepo, StockService stockService) {
        this.bonFournisseurRepo = bonFournisseurRepo;
        this.stockService = stockService;
    }

    @Override
    public BonFournisseur add(BonFournisseur bonFournisseur) {
        log.info("Service- BonFournisseurServiceImpl Calling add ");
        try {
            bonFournisseur = bonFournisseurRepo.save(bonFournisseur);
            insertStock_Fournisseur(bonFournisseur);
            return bonFournisseur;
        } catch (Exception e) {
            throw new OperationFailedException("l'ajout du bon a été suspendu, opération echouée", e);
        }
    }

    public void insertStock_Fournisseur(BonFournisseur bonFournisseur) {
        Stock stock = new Stock();
        stock.setChantier(bonFournisseur.getChantier());
        stock.setDate(bonFournisseur.getDate());
        stock.setEntreeF(bonFournisseur.getQuantite());
        if (stockService.getLastStock() != null)
            stock.setStockC(stockService.getLastStock().getStockC() + bonFournisseur.getQuantite());
        else stock.setStockC(bonFournisseur.getChantier().getStock());
        stockService.add(stock);
    }

    @Override
    public BonFournisseur getBon(Long id) {
        log.info("Service- BonFournisseurServiceImpl Calling getBon with param id = " + id);
        try {
            return bonFournisseurRepo.findById(id).get();
        } catch (NoSuchElementException e) {
            throw new ResourceNotFoundException("opération echouée , le bon n'existe pas ", e);
        } catch (Exception e) {
            throw new OperationFailedException("opération echouée , problème lors de l'extraction du bon ", e);
        }
    }

	@Override
	public BonFournisseur update(BonFournisseur bonFournisseur) {
        log.info("Service- BonFournisseurServiceImpl Calling update with param id = " + bonFournisseur.getId());
        try {
            BonFournisseur old = bonFournisseurRepo.findById(bonFournisseur.getId()).get();
            if (bonFournisseur.getCode() != null)
                old.setCode(bonFournisseur.getCode());
            if (bonFournisseur.getDate() != null)
                old.setDate(bonFournisseur.getDate());
            if (bonFournisseur.getPrixUnitaire() != null)
                old.setPrixUnitaire(bonFournisseur.getPrixUnitaire());
            if (bonFournisseur.getQuantite() != null)
                old.setQuantite(bonFournisseur.getQuantite());
            if (bonFournisseur.getChantier() != null)
                old.setChantier(bonFournisseur.getChantier());
            if (bonFournisseur.getFournisseur() != null)
                old.setFournisseur(bonFournisseur.getFournisseur());
            return bonFournisseurRepo.save(bonFournisseur);
        } catch (NoSuchElementException e) {
            throw new ResourceNotFoundException("opération echouée , le bon n'existe pas ", e);
        } catch (Exception e) {
            throw new OperationFailedException("opération echouée , problème lors de l'extraction du bon ", e);
        }

	}

	@Override
	public List<BonFournisseur> bonList() {
        log.info("Service- BonFournisseurServiceImpl Calling bonList ");
        try {
            return bonFournisseurRepo.findAll();
        } catch (NoSuchElementException e) {
            throw new ResourceNotFoundException("opération échouée , la liste des bons est vide", e);
        } catch (Exception e) {
            throw new OperationFailedException("opération echouée , l'extraction de la liste des bon a echouée", e);
        }
    }

	/**
	 * Search by code, date, fournisseur and chantier
	 *
	 * @param pageable
	 * @param search
	 * @return
	 */
	@Override
	public Page<BonFournisseur> bonList(Pageable pageable, String search) {
		log.info("Service- BonFournisseurServiceImpl Calling bonList with params :" + pageable + ", " + search);
		try {
			if (search.isEmpty()) {
				log.debug("fetching bonEngins page");
				return bonFournisseurRepo.findAll(pageable);
			} else {
				log.debug("Searching by :" + search);
				//creating example

				BonFournisseur bonFournisseur = new BonFournisseur();
				Chantier chantier = new Chantier();
				chantier.setNom(search);
				Fournisseur fournisseur = new Fournisseur();
				fournisseur.setNom(search);
				bonFournisseur.setChantier(chantier);
				bonFournisseur.setFournisseur(fournisseur);
				bonFournisseur.setCode(search);

				try {
					bonFournisseur.setDate(LocalDate.parse(search, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
				} catch (Exception e) {
					log.debug("Cannot search by date : keyword doesn't match date pattern");
				}

				//creating matcher
				ExampleMatcher matcher = ExampleMatcher.matchingAny()
						.withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
						.withIgnoreCase()
						.withIgnoreNullValues();
				Example<BonFournisseur> example = Example.of(bonFournisseur, matcher);
				log.debug("getting search results");
				return bonFournisseurRepo.findAll(example, pageable);
			}
		} catch (Exception e) {
			log.debug("Failed retrieving list of employes");
			throw new OperationFailedException("Operation échouée", e);
		}
	}

	@Override
	public void delete(Long id) {
        log.info("Service- BonFournisseurServiceImpl Calling delete with param id = " + id);
        try {
            bonFournisseurRepo.delete(bonFournisseurRepo.findById(id).get());
        } catch (NoSuchElementException e) {
            throw new ResourceNotFoundException("opération échouée , l'élement est introuvable", e);
        } catch (Exception e) {
            throw new OperationFailedException("opération echouée , la suppression du bon a echouée", e);
        }
    }
}