package yzh.stock.business.scheduler;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import yzh.stock.business.service.RecycleService;

@Component
@EnableScheduling
public class RecycleScheduler {

    private final RecycleService recycleService;

    public RecycleScheduler(RecycleService recycleService) {
        this.recycleService = recycleService;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredRecycle() {
        recycleService.cleanExpiredRecycle();
    }
}
