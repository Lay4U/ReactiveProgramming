package com.lay.reactiveprogramming.ch18;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final R2dbcEntityTemplate template;
    private final CustomBeanUtils<Book> beanUtils;

    public Mono<Book> saveBook(Book book){
        return verifyExistIsbn(book.getIsbn())
                .then(bookRepository.save(book));
    }

    public Mono<Book> updateBook(Book book) {
        return findVerifiedBook(book.getBookId())
                .map(findBook -> beanUtils.copyNonNullProperties(book, findBook))
                .flatMap(updatingbook -> bookRepository.save(updatingbook));
    }

    public Mono<Book> findBook(long bookId){
        return findVerifiedBook(bookId);
    }

    public Mono<List<Book>> findBooks() {
        return bookRepository.findAll().collectList();
    }

    public Mono<List<Book>> findBooksByPage(Long page, Long size) {
        return template
                .select(Book.class)
                .count()
                .flatMap(total -> {
                    Tuple2<Long, Long> limitAndOffset = getLimitAndOffset(total, page, size);
                    return template
                            .select(Book.class)
                            .all()
                            .skip(limitAndOffset.getT1())
                            .take(limitAndOffset.getT2())
                            .collectSortedList((Book b1, Book b2) ->
                                    (int) (b2.getBookId() - b1.getBookId()));
                });
    }

    private Tuple2<Long, Long> getLimitAndOffset(Long total, Long movePage, Long size) {
        long totalPages = (long) Math.ceil((double) total / size);
        long page = movePage > totalPages ? totalPages : movePage;
        long limit = total - (page * size) < 0 ? 0 : total - (page * size);
        long offset = total - (page * size) < 0 ? total - ((page - 1) * size) : size;

        return Tuples.of(limit, offset);
    }

    private Mono<Void> verifyExistIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .flatMap(findBook -> {
                    if (findBook != null) {
                        return Mono.error(new BusinessLogicException(ExceptionCode.BOOK_EXISTS))
                    }
                    return Mono.empty();
                });
    }

    private Mono<Book> findVerifiedBook(Long bookId){
        return bookRepository.findById(bookId)
                .switchIfEmpty(Mono.error(new BusinessLogicException(ExceptionCode.BOOK_NOT_FOUND)));
    }
}
