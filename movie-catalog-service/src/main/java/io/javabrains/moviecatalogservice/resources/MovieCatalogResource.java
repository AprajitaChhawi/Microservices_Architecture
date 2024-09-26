package io.javabrains.moviecatalogservice.resources;

import io.javabrains.moviecatalogservice.models.CatalogItem;
import io.javabrains.moviecatalogservice.models.Movie;
import io.javabrains.moviecatalogservice.models.Rating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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
    public List<CatalogItem> getCatalog(@PathVariable("userId") String userId){

        //1.get all rated movie ID's
        //2.for each movie id , call movieInfo service and get details
        //3.put it all together

        //1.hence this is hardcoded rating part (User has watched and rated these movies -> returned by ratings ms)
        List<Rating> ratings = Arrays.asList(
                new Rating("1234",9),
                new Rating("1211",3)
        );
        //2 . implementation of 2nd & 3rd part
        return ratings.stream().map(rating -> {
                    Movie movie = restTemplate.getForObject("http://localhost:8082/movies/" + rating.getMovieId(), Movie.class);
                    return new CatalogItem(movie.getName(), "Hello , this is test", rating.getRating());
                })
                .collect(Collectors.toList());

    }
}
