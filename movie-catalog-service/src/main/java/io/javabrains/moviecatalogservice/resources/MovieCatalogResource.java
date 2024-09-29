package io.javabrains.moviecatalogservice.resources;

import com.netflix.discovery.DiscoveryClient;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import io.javabrains.moviecatalogservice.models.CatalogItem;
import io.javabrains.moviecatalogservice.models.Movie;
import io.javabrains.moviecatalogservice.models.Rating;
import io.javabrains.moviecatalogservice.models.UserRating;
import io.javabrains.moviecatalogservice.services.MovieInfo;
import io.javabrains.moviecatalogservice.services.UserRatingInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/catalog")
public class MovieCatalogResource {

    @GetMapping("/v0/{userId}")
    public List<CatalogItem> getCatalogOlder(@PathVariable("userId") String user){
        return Collections.singletonList(
                new CatalogItem(
                        "Transformers","Test",4)
        );
    }

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("/{userId}")
    @HystrixCommand(fallbackMethod = "getFallbackCatalog")
    public List<CatalogItem> getCatalog(@PathVariable("userId") String userId){

        //1.get all rated movie ID's
        //2.for each movie id , call movieInfo service and get details
        //3.put it all together

        UserRating ratings = restTemplate.getForObject("http://ratings-data-service/ratingsdata/user/"+ userId , UserRating.class);
        //2 . implementation of 2nd & 3rd part
        return ratings.getUserRatings().stream().map(rating -> {
                    Movie movie = restTemplate.getForObject("http://movie-info-service/movies/" + rating.getMovieId(), Movie.class);
                    return new CatalogItem(movie.getName(), movie.getOverview(), rating.getRating());
                })
                .collect(Collectors.toList());

    }

    @Autowired
    private  WebClient.Builder webClientBuilder;

    @RequestMapping("/v2/{userId}")
    public List<CatalogItem> getCatalogUsingWebClient(@PathVariable("userId") String userId){

        List<Rating> ratings = Arrays.asList(
                new Rating("1234",9),
                new Rating("1211",3)
        );
        //2 . implementation of 2nd & 3rd part
        return ratings.stream().map(rating -> {
                    Movie movie = webClientBuilder.build()
                            .get()       //whatever the api type is
                            .uri("http://localhost:8082/movies/" + rating.getMovieId()) //url of ms
                            .retrieve()//fetch
                            .bodyToMono(Movie.class)//whatever body you get back , covert it into an instance of movie class
                            .block();  //we are waiting for this mono to return as return type of method is a list of catlogitems
                    return new CatalogItem(movie.getName(), "Hello , this is test", rating.getRating());
                })
                .collect(Collectors.toList());

    }

    @Autowired(required = true)
    MovieInfo movieInfo;

    @Autowired(required = true)
    UserRatingInfo userRatingInfo;
    //more granular approach for Hystrix behaviour

    @RequestMapping("/s1/{userId}")
    public List<CatalogItem> getCatalogNew(@PathVariable("userId") String userId) {
        UserRating ratings = userRatingInfo.getUserRating(userId);
        return ratings.getUserRatings().stream()
                .map(rating -> movieInfo.getCatalogItem(rating))
                .collect(Collectors.toList());
    }

    public List<CatalogItem> getFallbackCatalog(@PathVariable("userId") String userId){
        return Arrays.asList(new CatalogItem("No movie","",0));
    }

}
