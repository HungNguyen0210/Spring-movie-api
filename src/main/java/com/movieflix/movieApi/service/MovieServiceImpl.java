package com.movieflix.movieApi.service;

import com.movieflix.movieApi.dto.MovieDto;
import com.movieflix.movieApi.dto.MoviePageResponse;
import com.movieflix.movieApi.entities.Movie;
import com.movieflix.movieApi.exceptions.FileExistsException;
import com.movieflix.movieApi.exceptions.MovieNotFoundException;
import com.movieflix.movieApi.repositories.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;

    private final FileService fileService;

    @Value("${projects.poster}")
    private String path;

    @Value("${base.url}")
    private String baseUrl;

    public MovieServiceImpl(MovieRepository movieRepository, FileService fileService) {
        this.movieRepository = movieRepository;
        this.fileService = fileService;
    }

    @Override
    public MovieDto addMovie(MovieDto movieDto, MultipartFile file) throws IOException {
        //1. Tai file ve
        if (Files.exists(Paths.get(path + File.separator + file.getOriginalFilename()))) {
            throw new FileExistsException("File already exists");
        }
        String uploadedFileName = fileService.uploadFile(path, file);

        //2. De truong 'poster' thanh ten file
        movieDto.setPoster(uploadedFileName);

        //3. Map dto to movie object
        Movie movie = new Movie(movieDto.getMovieId(), movieDto.getTitle(), movieDto.getDirector(), movieDto.getStudio(), movieDto.getMovieCast(), movieDto.getReleaseYear(), movieDto.getPoster(), 0);

        //4.Luu movie object
        Movie savedMovie = movieRepository.save(movie);

        String posterUrl = baseUrl + "/file/" + uploadedFileName;

        MovieDto response = new MovieDto(savedMovie.getMovieId(), savedMovie.getTitle(), savedMovie.getDirector(), savedMovie.getStudio(), savedMovie.getMovieCast(), savedMovie.getReleaseYear(), savedMovie.getPoster(), posterUrl);

        return response;
    }

    @Override
    public MovieDto getMovie(Integer movieId) {
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new MovieNotFoundException("Movie not found"));

        String posterUrl = baseUrl + "/file/" + movie.getPoster();

        MovieDto response = new MovieDto(movie.getMovieId(), movie.getTitle(), movie.getDirector(), movie.getStudio(), movie.getMovieCast(), movie.getReleaseYear(), movie.getPoster(), posterUrl);

        return response;
    }

    @Override
    public List<MovieDto> getAllMovies() {
        List<Movie> movies = movieRepository.findAll();

        List<MovieDto> movieDtos = new ArrayList<>();

        for (Movie movie : movies) {
            String posterUrl = baseUrl + "/file/" + movie.getPoster();
            MovieDto movieDto = new MovieDto(movie.getMovieId(), movie.getTitle(), movie.getDirector(), movie.getStudio(), movie.getMovieCast(), movie.getReleaseYear(), movie.getPoster(), posterUrl);
            movieDtos.add(movieDto);
        }

        return movieDtos;
    }

    @Override
    public MovieDto updateMovie(Integer movieId, MovieDto movieDto, MultipartFile file) throws IOException {
        Movie mv = movieRepository.findById(movieId).orElseThrow(() -> new MovieNotFoundException("Movie not found"));

        String fileName = mv.getPoster();
        if (file != null) {
            Files.deleteIfExists(Paths.get(path + File.separator + fileName));
            fileName = fileService.uploadFile(path, file);
        }

        mv.setTitle(movieDto.getTitle());
        mv.setDirector(movieDto.getDirector());
        mv.setStudio(movieDto.getStudio());
        mv.setMovieCast(movieDto.getMovieCast());
        mv.setReleaseYear(movieDto.getReleaseYear());
        mv.setPoster(fileName);

        Movie updatedMovie = movieRepository.save(mv);
        String posterUrl = baseUrl + "/file/" + fileName;

        MovieDto response = new MovieDto(updatedMovie.getMovieId(), updatedMovie.getTitle(), updatedMovie.getDirector(), updatedMovie.getStudio(), updatedMovie.getMovieCast(), updatedMovie.getReleaseYear(), updatedMovie.getPoster(), posterUrl);
        return response;
    }

    @Override
    public String deleteMovie(Integer movieId) throws IOException {
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new MovieNotFoundException("Movie not found"));

        Files.deleteIfExists(Paths.get(path + File.separator + movie.getPoster()));

        movieRepository.delete(movie);
        return "Movie was deleted";
    }

    @Override
    public MoviePageResponse getAllMovieWithPagination(Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Movie> moviePages = movieRepository.findAll(pageable);
        List<Movie> movies = moviePages.getContent();

        List<MovieDto> movieDtos = new ArrayList<>();

        for (Movie movie : movies) {
            String posterUrl = baseUrl + "/file/" + movie.getPoster();
            MovieDto movieDto = new MovieDto(movie.getMovieId(), movie.getTitle(), movie.getDirector(), movie.getStudio(), movie.getMovieCast(), movie.getReleaseYear(), movie.getPoster(), posterUrl);
            movieDtos.add(movieDto);
        }

        return new MoviePageResponse(movieDtos, pageNumber, pageSize, moviePages.getTotalElements(), moviePages.getTotalPages(), moviePages.isLast());
    }

    @Override
    public MoviePageResponse getAllMovieWithPaginationAndSorting(Integer pageNumber, Integer pageSize, String sortBy, String dir) {
        Sort sort = dir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<Movie> moviePages = movieRepository.findAll(pageable);
        List<Movie> movies = moviePages.getContent();

        List<MovieDto> movieDtos = new ArrayList<>();

        for (Movie movie : movies) {
            String posterUrl = baseUrl + "/file/" + movie.getPoster();
            MovieDto movieDto = new MovieDto(movie.getMovieId(), movie.getTitle(), movie.getDirector(), movie.getStudio(), movie.getMovieCast(), movie.getReleaseYear(), movie.getPoster(), posterUrl);
            movieDtos.add(movieDto);
        }

        return new MoviePageResponse(movieDtos, pageNumber, pageSize, moviePages.getTotalElements(), moviePages.getTotalPages(), moviePages.isLast());
    }
}
