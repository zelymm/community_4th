package com.ll.exam;

import com.ll.exam.article.dto.ArticleDto;
import com.ll.exam.article.service.ArticleService;
import com.ll.exam.mymap.MyMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ArticleServiceTest {
    private MyMap myMap;
    private ArticleService articleService;
    private static final int TEST_DATA_SIZE = 100;

    public ArticleServiceTest() {
        myMap = Container.getObj(MyMap.class);
        articleService = Container.getObj(ArticleService.class);
    }

    @BeforeAll
    public void beforeAll() {
        myMap.setDevMode(true); // 모든 DB 처리시에, 처리되는 SQL을 콘솔에 출력
    }

    //@BeforeEach 메서드에 의해서 beforeEach 함수가 실행됨 -> @Test 메서드 실행 전 auto
    // -> remove article tables
    //모든 테스트들이 각각 독립적인 환경에서 실행 -> does not affect
    @BeforeEach
    public void beforeEach() {
        truncateArticleTable(); //DELETE FROM article;보다 TRUNCATE article;로 삭제하는게 더 깔끔
        makeArticleTestData(); // // 테스트에 필요한 샘플데이터를 만듦
    }

    private void makeArticleTestData() {
        IntStream.rangeClosed(1, TEST_DATA_SIZE).forEach(no -> {
            boolean isBlind = no >= 11 && no <= 20;
            String title = "제목%d".formatted(no);
            String body = "내용%d".formatted(no);

            myMap.run("""
                    INSERT INTO article
                    SET createdDate = NOW(),
                    modifiedDate = NOW(),
                    title = ?,
                    `body` = ?,
                    isBlind = ?
                    """, title, body, isBlind);
        });
    }

    private void truncateArticleTable() {
        myMap.run("TRUNCATE article");
    }

    @Test
    public void 존재한다() {
        assertThat(articleService).isNotNull();
    }

    @Test
    public void getArticles() {
        List<ArticleDto> articleDtoList = articleService.getArticles();
        assertThat(articleDtoList.size()).isEqualTo(TEST_DATA_SIZE);
    }

    @Test
    public void getArticleById() {
        ArticleDto articleDto = articleService.getArticleById(1);

        assertThat(articleDto.getId()).isEqualTo(1L);
        assertThat(articleDto.getTitle()).isEqualTo("제목1");
        assertThat(articleDto.getBody()).isEqualTo("내용1");
        assertThat(articleDto.getCreatedDate()).isNotNull();
        assertThat(articleDto.getModifiedDate()).isNotNull();
        assertThat(articleDto.isBlind()).isFalse();
    }

    @Test
    public void getArticlesCount() {
        // selectLong 메서드로*
        long articlesCount = articleService.getArticlesCount();

        assertThat(articlesCount).isEqualTo(TEST_DATA_SIZE);
    }

    @Test
    public void write(){
        long newArticleId = articleService.write("제목 new", "내용 new", false);

        ArticleDto articleDto = articleService.getArticleById(newArticleId);

        assertThat(articleDto.getId()).isEqualTo(newArticleId);
        assertThat(articleDto.getTitle()).isEqualTo("제목 new");
        assertThat(articleDto.getBody()).isEqualTo("내용 new");
        assertThat(articleDto.getCreatedDate()).isNotNull();
        assertThat(articleDto.getModifiedDate()).isNotNull();
        assertThat(articleDto.isBlind()).isEqualTo(false);
    }

    @Test
    public void modify(){
        articleService.modify(1, "제목 new", "내용 new", true);
        ArticleDto articleDto = articleService.getArticleById(1);

        assertThat(articleDto.getId()).isEqualTo(1);
        assertThat(articleDto.getTitle()).isEqualTo("제목 new");
        assertThat(articleDto.getBody()).isEqualTo("내용 new");
        assertThat(articleDto.isBlind()).isEqualTo(true);

        long diffSeconds = ChronoUnit.SECONDS.between(articleDto.getModifiedDate(), LocalDateTime.now());
        assertThat(diffSeconds).isLessThanOrEqualTo(1L);
    }

    @Test
    public void delete() {
        articleService.delete(1);
        ArticleDto articleDto = articleService.getArticleById(1);

        assertThat(articleDto).isNull();
    }

    @Test
    public void _2번글의_이전글은_1번글_이다() {
        ArticleDto id2ArticleDto = articleService.getArticleById(2);
        ArticleDto id1ArticleDto = articleService.getPrevArticle(id2ArticleDto);

        assertThat(id1ArticleDto.getId()).isEqualTo(1);
    }
    @Test
    public void _1번글의_이전글은_없다() {
        ArticleDto id1ArticleDto = articleService.getArticleById(1);
        ArticleDto nullArticleDto = articleService.getPrevArticle(id1ArticleDto);

        assertThat(nullArticleDto).isNull();
    }
    @Test
    public void _2번글의_다음글은_3번글_이다() {
        ArticleDto id3ArticleDto = articleService.getNextArticle(2);

        assertThat(id3ArticleDto.getId()).isEqualTo(3);
    }

    @Test
    public void 마지막글의_다음글은_없다() {
        long lastArticleId = TEST_DATA_SIZE;
        ArticleDto nullArticleDto = articleService.getNextArticle(lastArticleId);

        assertThat(nullArticleDto).isNull();
    }
    @Test
    public void _10번글의_다음글은_21번글_이다_왜냐하면_11번글부터_20번글까지는_블라인드라서() {
        ArticleDto nextArticleDto = articleService.getNextArticle(10);

        assertThat(nextArticleDto.getId()).isEqualTo(21);
    }
}
