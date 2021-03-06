package me.kadarh.mecaworks.repo.others;

import me.kadarh.mecaworks.domain.others.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StockRepo extends JpaRepository<Stock, Long> {

    @Query(nativeQuery = true, value = "select * from stock order by updated_at desc limit 1")
    Stock findLastStock();
}
