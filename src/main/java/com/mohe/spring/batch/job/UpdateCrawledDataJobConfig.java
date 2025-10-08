package com.mohe.spring.batch.job;

import com.mohe.spring.dto.crawling.CrawledDataDto;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.PlaceBusinessHour;
import com.mohe.spring.entity.PlaceDescription;
import com.mohe.spring.entity.PlaceImage;
import com.mohe.spring.entity.PlaceSns;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.service.crawling.CrawlingService;
import com.mohe.spring.service.image.ImageService;
import com.mohe.spring.service.OllamaService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
public class UpdateCrawledDataJobConfig {

    private final CrawlingService crawlingService;
    private final OllamaService ollamaService;
    private final ImageService imageService;
    private final PlaceRepository placeRepository;

    public UpdateCrawledDataJobConfig(CrawlingService crawlingService, OllamaService ollamaService, ImageService imageService, PlaceRepository placeRepository) {
        this.crawlingService = crawlingService;
        this.ollamaService = ollamaService;
        this.imageService = imageService;
        this.placeRepository = placeRepository;
    }

    @Bean
    public Job updateCrawledDataJob(JobRepository jobRepository, Step updateCrawledDataStep) {
        return new JobBuilder("updateCrawledDataJob", jobRepository)
                .start(updateCrawledDataStep)
                .build();
    }

    @Bean
    public Step updateCrawledDataStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, ItemReader<Place> placeReader, ItemProcessor<Place, Place> placeProcessor, ItemWriter<Place> placeWriter) {
        return new StepBuilder("updateCrawledDataStep", jobRepository)
                .<Place, Place>chunk(10, transactionManager)
                .reader(placeReader)
                .processor(placeProcessor)
                .writer(placeWriter)
                .build();
    }

    @Bean
    public RepositoryItemReader<Place> placeReader() {
        RepositoryItemReader<Place> reader = new RepositoryItemReader<>();
        reader.setRepository(placeRepository);
        reader.setMethodName("findByReady");
        reader.setArguments(List.of(false));
        reader.setPageSize(10);
        reader.setSort(Map.of("id", Sort.Direction.ASC));
        return reader;
    }

    @Bean
    public ItemProcessor<Place, Place> placeProcessor() {
        return place -> {
            // Get search query from PlaceDescription if available
            String searchQuery = place.getRoadAddress();
            if (!place.getDescriptions().isEmpty()) {
                String savedSearchQuery = place.getDescriptions().get(0).getSearchQuery();
                if (savedSearchQuery != null && !savedSearchQuery.isEmpty()) {
                    searchQuery = savedSearchQuery;
                }
            }

            CrawledDataDto crawledData = crawlingService.crawlPlaceData(searchQuery, place.getName()).block().getData();

            // Update Place entity with crawled data
            try {
                place.setReviewCount(Integer.parseInt(crawledData.getReviewCount()));
            } catch (NumberFormatException e) {
                place.setReviewCount(0);
            }
            place.setParkingAvailable(crawledData.isParkingAvailable());
            place.setPetFriendly(crawledData.isPetFriendly());

            // Clear and create new PlaceDescription
            place.getDescriptions().clear();
            PlaceDescription description = new PlaceDescription();
            description.setPlace(place);
            description.setOriginalDescription(crawledData.getOriginalDescription());
            description.setAiSummary(String.join("\n", crawledData.getAiSummary()));
            description.setSearchQuery(searchQuery);

            // Generate Mohe description using Ollama
            String categoryStr = place.getCategory() != null ? String.join(",", place.getCategory()) : "";
            String moheDescription = ollamaService.generateMoheDescription(
                String.join("\n", crawledData.getAiSummary()),
                categoryStr,
                place.getPetFriendly() != null ? place.getPetFriendly() : false
            );
            description.setOllamaDescription(moheDescription);
            place.getDescriptions().add(description);

            // Generate and set keywords using Ollama
            String[] keywords = ollamaService.generateKeywords(
                String.join("\n", crawledData.getAiSummary()),
                categoryStr,
                place.getPetFriendly() != null ? place.getPetFriendly() : false
            );
            place.setKeyword(Arrays.asList(keywords));

            // Vectorize keywords using Ollama embedding
            float[] keywordVector = ollamaService.vectorizeKeywords(keywords);
            place.setKeywordVector(Arrays.toString(keywordVector));

            // Download and save images
            place.getImages().clear();
            if (crawledData.getImageUrls() != null && !crawledData.getImageUrls().isEmpty()) {
                List<String> savedImagePaths = imageService.downloadAndSaveImages(
                    place.getId(),
                    place.getName(),
                    crawledData.getImageUrls()
                );

                for (int i = 0; i < savedImagePaths.size(); i++) {
                    PlaceImage placeImage = new PlaceImage();
                    placeImage.setPlace(place);
                    placeImage.setUrl(savedImagePaths.get(i));
                    placeImage.setOrderIndex(i + 1);
                    place.getImages().add(placeImage);
                }
            }

            // Create and set PlaceBusinessHours
            place.getBusinessHours().clear();
            if (crawledData.getBusinessHours() != null && crawledData.getBusinessHours().getWeekly() != null) {
                for (Map.Entry<String, com.mohe.spring.dto.crawling.WeeklyHoursDto> entry : crawledData.getBusinessHours().getWeekly().entrySet()) {
                    PlaceBusinessHour businessHour = new PlaceBusinessHour();
                    businessHour.setPlace(place);
                    businessHour.setDayOfWeek(entry.getKey());

                    // Parse time strings safely
                    try {
                        if (entry.getValue().getOpen() != null && !entry.getValue().getOpen().isEmpty()) {
                            businessHour.setOpen(LocalTime.parse(entry.getValue().getOpen()));
                        }
                        if (entry.getValue().getClose() != null && !entry.getValue().getClose().isEmpty()) {
                            businessHour.setClose(LocalTime.parse(entry.getValue().getClose()));
                        }
                    } catch (Exception e) {
                        // Log and continue if time parsing fails
                        System.err.println("Failed to parse business hours for " + place.getName() + ": " + e.getMessage());
                    }

                    businessHour.setDescription(entry.getValue().getDescription());
                    businessHour.setIsOperating(entry.getValue().isOperating());

                    // Set last order minutes from the parent business_hours object
                    if (crawledData.getBusinessHours().getLastOrderMinutes() != null) {
                        businessHour.setLastOrderMinutes(crawledData.getBusinessHours().getLastOrderMinutes());
                    }

                    place.getBusinessHours().add(businessHour);
                }
            }

            // Create and set PlaceSns
            place.getSns().clear();
            if (crawledData.getSnsUrls() != null && !crawledData.getSnsUrls().isEmpty()) {
                for (Map.Entry<String, String> entry : crawledData.getSnsUrls().entrySet()) {
                    if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                        PlaceSns sns = new PlaceSns();
                        sns.setPlace(place);
                        sns.setPlatform(entry.getKey());
                        sns.setUrl(entry.getValue());
                        place.getSns().add(sns);
                    }
                }
            }

            // Mark place as ready after successful processing
            place.setReady(true);

            return place;
        };
    }

    @Bean
    public ItemWriter<Place> placeWriter() {
        return places -> placeRepository.saveAll(places);
    }
}