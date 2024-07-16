package com.lec.spring.service;

import com.lec.spring.domain.Attachment;
import com.lec.spring.domain.Post;
import com.lec.spring.domain.User;
import com.lec.spring.repository.AttachmentRepository;
import com.lec.spring.repository.PostRepository;
import com.lec.spring.repository.UserRepository;
import com.lec.spring.util.U;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@Service
public class BoardServiceImpl implements BoardService {

    @Value("${app.upload.path}")
    private String uploadDir;

    @Value("${app.pagination.write_pages}")
    private int WRITE_PAGES;

    @Value("${app.pagination.page_rows}")
    private int PAGE_ROWS;

    private PostRepository postRepository;
    private UserRepository userRepository;
    private AttachmentRepository attachmentRepository;

    @Autowired
    public void setPostRepository(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setAttachmentRepository(AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    // 특정 글(id) 에 첨부파일 추가
    private void addFiles(Map<String, MultipartFile> files, Long id) {
        if (files == null) return;

        for(Map.Entry<String, MultipartFile> e : files.entrySet()) {

            // name="upfile##" 인 경우만 첨부파일 등록. (이유, 다른 웹에디터와 섞이지 않도록..ex: summernote)
            if(!e.getKey().startsWith("upfile")) continue;

            // 첨부파일 정보 출력 ( 학습목적 )
            System.out.println("\n첨부파일 정보: " + e.getKey()); // name 값
            U.printfileInfo(e.getValue()); // MultipartFile 정보
            System.out.println();

            // 물리적인 파일 저장
            Attachment file = upload(e.getValue());

            // 성공하면 DB 에도 저장되야함
            if (file != null) {
                file.setPost(id); // FK 설정
                attachmentRepository.save(file); // INSERT
            }

        }
    } // end addFiles()

    // 물리적으로 서버에 파일을 저장하고 중복된 파일이름 -> rename 처리
    private Attachment upload(MultipartFile multipartFile) {
        Attachment attachment = null;

        // 첨부된 파일 없으면 pass
        String originalFilename = multipartFile.getOriginalFilename(); // 원본이름
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return null;
        }

        // 원본 파일명
        //  ※ cleanPath 는 C:\Users\aaa\bbbb\dsaf\asdfsafd.ddd
        //                  "\" -> "/" 로 변경해줌
        String sourceName = StringUtils.cleanPath(multipartFile.getOriginalFilename());

        // 저장될 파일 명
        String fileName = sourceName;

        // 파일이 중복되는지 확인
        File file = new File(uploadDir, fileName);
        if (file.exists()){ // 참이면 이미 존재하는 파일명, 중복된다면 다른 이름으로 변경해서 저장
            // a.txt => a_2378142783946.txt  : time stamp 값을 활용할거다!
            // "a" => "a_2378142783946"  : 확장자 없는 경우

           int pos = fileName.lastIndexOf(".");
           if (pos > -1) { // 확장자가 있는 경우
               String name = fileName.substring(0, pos); // 파일 '이름'
               String ext = fileName.substring(pos + 1); // 파일 '확장자'

               // 중복방지 회피를 위해 새로운 이름 (time stamp, 현재시간 ms) 를 파일명에 추가
               fileName = name + "_" + System.currentTimeMillis() + "." + ext;

           } else { // 확장자가 없는 파일의 경우
               fileName += "_" + System.currentTimeMillis();
           }
        }

        // 저장 할 파일명
        System.out.println("fileName = " + fileName);

        // java.io.*
        // java.nio.* ( new io 객체 -> Path, Paths, Files ...등)
        Path copyOfLocation = Paths.get(new File(uploadDir, fileName).getAbsolutePath()); // 경로설정
        System.out.println(copyOfLocation);

        try {
            // inputStream을 가져와서
            // copyOfLocation (저장위치)로 파일을 쓴다.
            // copy의 옵션은 기존에 존재하면 REPLACE(대체한다), 오버라이딩 한다

            // java.nio.file.Files
            Files.copy(
                    multipartFile.getInputStream(),
                    copyOfLocation,
                    StandardCopyOption.REPLACE_EXISTING // 기존에 존재하면 덮어쓰기

            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        attachment = Attachment.builder()
                .filename(fileName) // 저장된 이름
                .sourcename(sourceName) // 원본 이름
                .build();

        return attachment;
    }

    // [첨부파일이 이미지 파일인지 아닌지 여부 세팅 메소드]
    private void setImage(List<Attachment> fileList) {
        // upload 실제 물리적인 경로
        String realPath = new File(uploadDir).getAbsolutePath(); // 절대경로

        for (Attachment attachment : fileList){
            BufferedImage imgData = null;
            File f = new File(realPath, attachment.getFilename()); // 저장된 첨부파일에 대한 File 객체

            try {
                imgData = ImageIO.read(f);
            // ※ ↑ 파일이 존재 하지 않으면 IOExcepion 발생한다
            //   ↑ 이미지가 아닌 경우는 null 리턴

            // 이미지 인 경우 세팅
            if (imgData != null) {attachment.setImage(true);}

            } catch (IOException e) {
                System.out.println("파일존재안함: " + f.getAbsolutePath() + " [" + e.getMessage() + "]");
            }
        }
    }

    // 특정 첨부파이 물리적으로 삭제하기
    private void delFile(Attachment file) {
        String saveDirectory = new File(uploadDir).getAbsolutePath(); // 파일이 저장된 전체 경로

        File f = new File(saveDirectory, file.getFilename()); // 물리적으로 저장된 파일
        System.out.println("삭제시도--> " + f.getAbsolutePath());

        if (f.exists()){
            if (f.delete()){
                System.out.println("삭제 성공");
            } else {
                System.out.println("삭제 실패");
            }
        } else {
            System.out.println("파일이 존재하지 않습니다.");
        }
    }


    @Override
    public int write(Post post, Map<String, MultipartFile> files) {
        User user = U.getLoggedUser();

        user = userRepository.findById(user.getId()).orElse(null);
        post.setUser(user);

        post = postRepository.saveAndFlush(post);
        addFiles(files, post.getId());
        return 1;
    }

    @Override
    public Post detail(Long id) {

        Post post = postRepository.findById(id).orElse(null);

        if (post != null) {
            post.setViewCnt(post.getViewCnt() + 1);
            postRepository.saveAndFlush(post);

            List<Attachment> fileList = attachmentRepository.findByPost(post.getId());
            setImage(fileList);
            post.setFileList(fileList);
        }
        return post;
    }

    @Override
    public List<Post> list() {
        return postRepository.findAll();
    }

    @Override
    public List<Post> list(Integer page, Model model) {
        // 현재 페이지
        if(page == null || page < 1) page = 1;   // 디폴트 1page

// 페이징
// writePages: 한 [페이징] 당 몇개의 페이지가 표시되나
// pageRows: 한 '페이지'에 몇개의 글을 리스트 할것인가?
        HttpSession session = U.getSession();
        Integer writePages = (Integer)session.getAttribute("writePages");
        if(writePages == null) writePages = WRITE_PAGES;
        Integer pageRows = (Integer)session.getAttribute("pageRows");
        if(pageRows == null) pageRows = PAGE_ROWS;
        session.setAttribute("page", page);   // 현재 페이지 번호 -> session 에 저장

        // 주의! -> PageRequest.of() 의 page 값은 0-base 다!
        Page<Post> pagePost = postRepository.findAll(PageRequest.of(page - 1, pageRows, Sort.by(Sort.Order.desc("id"))));
        long cnt = pagePost.getTotalElements();  // 글 목록 전체의 개수
        int totalPage = pagePost.getTotalPages();  // 총 몇 '페이지' 분량?

// [페이징] 에 표시할 '시작페이지' 와 '마지막 페이지'
        int startPage = 0;
        int endPage = 0;

// 해당 '페이지' 의 글 목록
        List<Post> list = null;

        if(cnt > 0){  // 데이터가 최소 1개 이상 있는 경우만 페이징

            // page 값 보정
            if(page > totalPage) page = totalPage;

            // fromRow : 몇번째 데이터부터
            int fromRow = (page - 1) * pageRows;

            // [페이징] 에 표시할 '시작페이지' 와 '마지막페이지' 계산
            startPage = (((page - 1) / writePages) * writePages) + 1;
            endPage = startPage + writePages - 1;
            if (endPage >= totalPage) endPage = totalPage;

            // 해당 페이지의 글 목록 읽어오기
            list = pagePost.getContent();
            model.addAttribute("list", list);
        } else {
            page = 0;
        }

        model.addAttribute("cnt", cnt);  // 전체 글 개수
        model.addAttribute("page", page); // 현재 페이지
        model.addAttribute("totalPage", totalPage);  // 총 '페이지' 수
        model.addAttribute("pageRows", pageRows);  // 한 '페이지' 에 표시할 글 개수

        // [페이징]
        model.addAttribute("url", U.getRequest().getRequestURI());  // 목록 url
        model.addAttribute("writePages", writePages); // [페이징] 에 표시할 숫자 개수
        model.addAttribute("startPage", startPage);  // [페이징] 에 표시할 시작 페이지
        model.addAttribute("endPage", endPage);   // [페이징] 에 표시할 마지막 페이지

        return list;
    }


    @Override
    public Post selectById(Long id) {
        Post post = postRepository.findById(id).orElse(null);

        if (post != null) {
            List<Attachment> fileList = attachmentRepository.findByPost(post.getId());
            setImage(fileList);
            post.setFileList(fileList);
        }
        return post;
    }

    @Override
    public int update(Post post, Map<String, MultipartFile> files, Long[] delfile) {
        int result = 0;

        Post p = postRepository.findById(post.getId()).orElse(null);

        if (p != null) {
            p.setSubject(post.getSubject());
            p.setContent(post.getContent());
            p = postRepository.saveAndFlush(p);

            addFiles(files, p.getId());

            if (delfile != null) {
                for (Long fileId : delfile) {
                    Attachment file = attachmentRepository.findById(fileId).orElse(null);
                    if (file != null) {
                        delFile(file);
                        attachmentRepository.delete(file);
                    }
                }
            }
            result = 1;
        }
        return result;
    }

    @Override
    public int deleteById(Long id) {
        int result = 0;

        Post post = postRepository.findById(id).orElse(null);
        if (post != null) {
            List<Attachment> fileList = attachmentRepository.findByPost(id);

            if (fileList != null && fileList.size() > 0) {
                for (Attachment file : fileList) {
                    delFile(file);
                }
            }
            postRepository.delete(post);
            result = 1;
        }
        return result;
    }
} // end Service


































