package com.ll.exam;

import com.ll.exam.article.dto.ArticleDto;
import com.ll.exam.article.service.ArticleService;
import com.ll.exam.mymap.MyMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ArticleServiceTest {

    //@BeforeEach 메서드에 의해서 beforeEach 함수가 실행됨 -> @Test 메서드 실행 전 auto
    // -> remove article tables
    //모든 테스트들이 각각 독립적인 환경에서 실행 -> does not affect
    @BeforeEach
    public void beforeEach() {
        truncateArticleTable(); //DELETE FROM article;보다 TRUNCATE article;로 삭제하는게 더 깔끔
        makeArticleTestData(); // // 테스트에 필요한 샘플데이터를 만듦
    }

    private void makeArticleTestData() {
        MyMap myMap = Container.getObj(MyMap.class);
        IntStream.rangeClosed(1, 3).forEach(no -> {
            boolean isBlind = false;
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
        MyMap myMap = Container.getObj(MyMap.class);
        myMap.run("TRUNCATE article");
    }

    @Test
    public void 존재한다() {
        ArticleService articleService = Container.getObj(ArticleService.class);

        assertThat(articleService).isNotNull();
    }

    @Test
    public void getArticles() {
        ArticleService articleService = Container.getObj(ArticleService.class);

        List<ArticleDto> articleDtoList = articleService.getArticles();
        assertThat(articleDtoList.size()).isEqualTo(3);
    }

    @Test
    public void getArticleById() {
        ArticleService articleService = Container.getObj(ArticleService.class);
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
        ArticleService articleService = Container.getObj(ArticleService.class);
        // selectLong 메서드로*
        long articlesCount = articleService.getArticlesCount();

        assertThat(articlesCount).isEqualTo(3);
    }

    @Test
    public void write(){
        ArticleService articleService = Container.getObj(ArticleService.class);

        long newArticleId = articleService.write("제목 new", "내용 new", false);

        ArticleDto articleDto = articleService.getArticleById(newArticleId);

        assertThat(articleDto.getId()).isEqualTo(newArticleId);
        assertThat(articleDto.getTitle()).isEqualTo("제목 new");
        assertThat(articleDto.getBody()).isEqualTo("내용 new");
        assertThat(articleDto.getCreatedDate()).isNotNull();
        assertThat(articleDto.getModifiedDate()).isNotNull();
        assertThat(articleDto.isBlind()).isEqualTo(false);
    }
}
