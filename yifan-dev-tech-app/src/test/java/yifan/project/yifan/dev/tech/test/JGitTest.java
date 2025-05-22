package yifan.project.yifan.dev.tech.test;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.PathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;


@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class JGitTest {

    @Resource
    private OllamaChatClient ollamaChatClient;

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private SimpleVectorStore simpleVectorStore;

    @Resource
    private PgVectorStore pgVectorStore;

    @Test
    public void test() throws Exception {
        // 这部分替换为你的
        String repoURL = "https://github.com/Fanyiichi/yi-big-market";
        String username = "Fanyiichi";
        String password = "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";

        String localPath = "./cloned-repo";
        log.info("克隆路径：" + new File(localPath).getAbsolutePath());

        FileUtils.deleteDirectory(new File(localPath));

        Git git = Git.cloneRepository()
                .setURI(repoURL)
                .setDirectory(new File(localPath))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                .call();

        git.close();
    }

    @Test
    public void test_file() throws IOException {
        Files.walkFileTree(Paths.get("./cloned-repo"), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String filePath = file.toString();

                // ① 跳过 .git 文件夹及内部内容
                if (filePath.contains(".git")) {
                    return FileVisitResult.CONTINUE;
                }

                // ② 可选：只处理文本类文件
                if (!(filePath.endsWith(".md") || filePath.endsWith(".txt") || filePath.endsWith(".java"))) {
                    return FileVisitResult.CONTINUE;
                }

                log.info("文件路径:{}", filePath);

                try {
                    PathResource resource = new PathResource(file);
                    TikaDocumentReader reader = new TikaDocumentReader(resource);

                    List<Document> documents = reader.get();
                    List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

                    documents.forEach(doc -> doc.getMetadata().put("knowledge", "yi-big-market"));
                    documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", "yi-big-market"));

                    pgVectorStore.accept(documentSplitterList);
                } catch (Exception e) {
                    log.warn("处理文件失败: {}", filePath, e);
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

}
