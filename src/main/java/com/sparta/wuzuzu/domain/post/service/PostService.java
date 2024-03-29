package com.sparta.wuzuzu.domain.post.service;

import com.sparta.wuzuzu.domain.post.dto.PostRequest;
import com.sparta.wuzuzu.domain.post.dto.PostResponse;
import com.sparta.wuzuzu.domain.post.dto.StuffPostProjection;
import com.sparta.wuzuzu.domain.post.entity.Post;
import com.sparta.wuzuzu.domain.post.repository.PostRepository;
import com.sparta.wuzuzu.domain.post.repository.query.PostQueryRepository;
import com.sparta.wuzuzu.domain.stuff.entity.Stuff;
import com.sparta.wuzuzu.domain.stuff.repository.StuffRepository;
import com.sparta.wuzuzu.domain.user.entity.User;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;
    private final StuffRepository stuffRepository;


    @Transactional
    public void createPost(
        User user,
        PostRequest requestDto
    ) {

        Stuff stuff = stuffRepository.findByUserAndStuffId(user, requestDto.getStuffId()).orElseThrow(
            () -> new IllegalArgumentException("Stuff is empty."));
        /*
        Stuff stuff = stuffRepository.findById(requestDto.getStuffId()).orElseThrow(
            () -> new IllegalArgumentException("stuff is empty.")
        );

        if(!user.getUserId().equals(stuff.getUser().getUserId())){
            throw new IllegalArgumentException("stuff is not yours.");
        }
         */

        if(stuff.getStock() < requestDto.getCount()){
            throw new IllegalArgumentException("stuff is out of stock");
        }

        postRepository.save(new Post(requestDto, user));
    }

    // 전체 게시글 : 제목, 조회수만 출력
    // 전체 게시글 목록 조회 : createAt 기준으로 pageable 사용하기(미적용)
    @Transactional
    public List<PostResponse> getPosts()
    {
        List<Post> postList = postRepository.findAllByStatusTrue();

        return postList.stream()
            .map(PostResponse::new)
            .collect(Collectors.toList());
    }

    // 상세 게시글 : 제목, 조회수, 설명, 상품 내용 출력
    // 게시물 상세 조회 : QueryDSL 사용하기
    @Transactional
    public StuffPostProjection getPost(
        Long postId
    ) {
        Post post = postRepository.findById(postId).orElseThrow(
            () -> new IllegalArgumentException("post is empty.")
        );

        if(!post.getStatus()){
            throw new IllegalArgumentException("post is deleted");
        }

        post.increaseViews();   // 동시성 제어 고려

        return postQueryRepository.findStuffAndPostByPostId(postId);
    }

    @Transactional
    public void updatePost(
        User user,
        Long postId,
        PostRequest requestDto
    ) {
        Post post = postRepository.findById(postId).orElseThrow(
            () -> new IllegalArgumentException("post is empty.")
        );

        if(!user.getUserId().equals(post.getUser().getUserId())){
            throw new IllegalArgumentException("post is not yours");
        }

        if(!post.getStatus()){
            throw new IllegalArgumentException("post is deleted.");
        }

        Stuff stuff = stuffRepository.findById(requestDto.getStuffId()).orElseThrow(
            () -> new IllegalArgumentException("stuff is empty.")
        );

        if(!user.getUserId().equals(stuff.getUser().getUserId())){
            throw new IllegalArgumentException("stuff is not yours");
        }

        post.update(requestDto);
    }

    @Transactional
    public void deletePost(
        User user,
        Long postId
    ) {
        Post post = postRepository.findById(postId).orElseThrow(
            () -> new IllegalArgumentException("post is empty.")
        );

        if(!user.getUserId().equals(post.getUser().getUserId())){
            throw new IllegalArgumentException("post is not yours");
        }

        if(!post.getStatus()){
            throw new IllegalArgumentException("post is already deleted.");
        }

        post.delete();
    }
}
