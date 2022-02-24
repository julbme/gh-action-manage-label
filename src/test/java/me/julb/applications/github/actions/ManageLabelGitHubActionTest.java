/**
 * MIT License
 *
 * Copyright (c) 2017-2022 Julb
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.julb.applications.github.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletionException;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHLabel.Updater;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.LocalPagedIterable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import me.julb.sdk.github.actions.kit.GitHubActionsKit;

/**
 * Test class for {@link ManageLabelGitHubAction} class. <br>
 * @author Julb.
 */
@ExtendWith(MockitoExtension.class)
@Testcontainers
class ManageLabelGitHubActionTest {

    /**
     * The class under test.
     */
    private ManageLabelGitHubAction githubAction = null;

    /**
     * A mock for GitHub action kit.
     */
    @Mock
    private GitHubActionsKit ghActionsKitMock;

    /**
     * A mock for GitHub API.
     */
    @Mock
    private GitHub ghApiMock;

    /**
     * A mock for GitHub repository.
     */
    @Mock
    private GHRepository ghRepositoryMock;

    /**
     * A nginx server
     */
    //@formatter:off
    public NginxContainer<?> nginx = new NginxContainer<>(DockerImageName.parse("nginx"))
        .withClasspathResourceMapping("labels/", "/usr/share/nginx/html", BindMode.READ_ONLY)
        .waitingFor(new HttpWaitStrategy());
    //@formatter:on

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp()
        throws Exception {
        githubAction = new ManageLabelGitHubAction();
        githubAction.setGhActionsKit(ghActionsKitMock);
        githubAction.setGhApi(ghApiMock);
        githubAction.setGhRepository(ghRepositoryMock);
    }

    /**
     * Test method.
     */
    @Test
    void whenGetInputFrom_thenReturnValue()
        throws Exception {
        when(this.ghActionsKitMock.getRequiredMultilineInput("from")).thenReturn(new String[] {"file1.yml", "file2.json"});

        assertThat(this.githubAction.getInputFrom()).containsExactly("file1.yml", "file2.json");

        verify(this.ghActionsKitMock).getRequiredMultilineInput("from");
    }

    /**
     * Test method.
     */
    @Test
    void whenGetInputFromNotProvided_thenFail() {
        when(this.ghActionsKitMock.getRequiredMultilineInput("from")).thenThrow(NoSuchElementException.class);
        assertThrows(CompletionException.class, () -> this.githubAction.execute());
        verify(this.ghActionsKitMock).getRequiredMultilineInput("from");
    }

    /**
     * Test method.
     */
    @Test
    void whenGetInputSkipDeleteProvided_thenReturnValue()
        throws Exception {
        when(this.ghActionsKitMock.getBooleanInput("skip_delete")).thenReturn(Optional.of(true));

        assertThat(this.githubAction.getInputSkipDelete()).isTrue();

        verify(this.ghActionsKitMock).getBooleanInput("skip_delete");
    }

    /**
     * Test method.
     */
    @Test
    void whenGetInputSkipDeleteNotProvided_thenReturnDefaultValue()
        throws Exception {
        when(this.ghActionsKitMock.getBooleanInput("skip_delete")).thenReturn(Optional.empty());

        assertThat(this.githubAction.getInputSkipDelete()).isFalse();

        verify(this.ghActionsKitMock).getBooleanInput("skip_delete");
    }

    /**
     * Test method.
     */
    @Test
    void whenExecuteManageLabelsWithSkipDelete_thenLabelCreatedUpdatedNotDeleted()
        throws Exception {
        var spy = spy(this.githubAction);

        var label1 = new LabelDTO("label1", "000000");
        var label2 = new LabelDTO("label2", "111111", "some desc");
        var ghLabel1 = mock(GHLabel.class);
        var ghLabel3 = mock(GHLabel.class);

        when(this.ghActionsKitMock.getGitHubRepository()).thenReturn("octocat/Hello-World");
        doReturn(new String[] {"file1.yml"}).when(spy).getInputFrom();
        doReturn(true).when(spy).getInputSkipDelete();

        doNothing().when(spy).connectApi();
        doReturn(Map.of("label1", label1, "label2", label2)).when(spy).getInputLabels(new String[] {"file1.yml"});
        doReturn(Map.of("label1", ghLabel1, "label3", ghLabel3)).when(spy).getGHLabels();
        doNothing().when(spy).createLabels(new TreeSet<>(List.of(label2)));
        doNothing().when(spy).updateLabels(new TreeMap<>(Map.of(label1, ghLabel1)));

        when(this.ghApiMock.getRepository("octocat/Hello-World")).thenReturn(ghRepositoryMock);

        spy.execute();

        verify(this.ghActionsKitMock).getGitHubRepository();

        verify(spy).getInputFrom();
        verify(spy).getInputSkipDelete();
        verify(spy).connectApi();
        verify(spy).createLabels(new TreeSet<>(List.of(label2)));
        verify(spy).updateLabels(new TreeMap<>(Map.of(label1, ghLabel1)));
        verify(spy, never()).deleteLabels(anyCollection());

        verify(this.ghApiMock).getRepository("octocat/Hello-World");
    }

    /**
     * Test method.
     */
    @Test
    void whenExecuteManageLabelsWithoutSkipDelete_thenLabelCreatedUpdatedDeleted()
        throws Exception {
        var spy = spy(this.githubAction);

        var label1 = new LabelDTO("label1", "000000");
        var label2 = new LabelDTO("label2", "111111", "some desc");
        var ghLabel1 = mock(GHLabel.class);
        var ghLabel3 = mock(GHLabel.class);

        when(this.ghActionsKitMock.getGitHubRepository()).thenReturn("octocat/Hello-World");
        doReturn(new String[] {"file1.yml"}).when(spy).getInputFrom();
        doReturn(false).when(spy).getInputSkipDelete();

        doNothing().when(spy).connectApi();
        doReturn(Map.of("label1", label1, "label2", label2)).when(spy).getInputLabels(new String[] {"file1.yml"});
        doReturn(Map.of("label1", ghLabel1, "label3", ghLabel3)).when(spy).getGHLabels();
        doNothing().when(spy).createLabels(new TreeSet<>(List.of(label2)));
        doNothing().when(spy).updateLabels(new TreeMap<>(Map.of(label1, ghLabel1)));
        doNothing().when(spy).deleteLabels(new ArrayList<>(List.of(ghLabel3)));

        when(this.ghApiMock.getRepository("octocat/Hello-World")).thenReturn(ghRepositoryMock);

        spy.execute();

        verify(this.ghActionsKitMock).getGitHubRepository();

        verify(spy).getInputFrom();
        verify(spy).getInputSkipDelete();
        verify(spy).connectApi();
        verify(spy).createLabels(new TreeSet<>(List.of(label2)));
        verify(spy).updateLabels(new TreeMap<>(Map.of(label1, ghLabel1)));
        verify(spy).deleteLabels(new ArrayList<>(List.of(ghLabel3)));

        verify(this.ghApiMock).getRepository("octocat/Hello-World");
    }

    // /**
    // * Test method.
    // */
    // @Test
    // void whenExecuteOpenMilestoneNotExists_thenMilestoneOpened()
    // throws Exception {
    // var spy = spy(this.githubAction);
    //
    // var ghMilestoneCreated = Mockito.mock(GHMilestone.class);
    // when(ghMilestoneCreated.getNumber()).thenReturn(123);
    //
    // var dueOn = new Date();
    //
    // when(this.ghActionsKitMock.getGitHubRepository()).thenReturn("octocat/Hello-World");
    // doReturn("v1.0.0").when(spy).getInputTitle();
    // doReturn(InputMilestoneState.OPEN).when(spy).getInputState();
    // doReturn(Optional.of("description")).when(spy).getInputDescription();
    // doReturn(Optional.of(dueOn)).when(spy).getInputDueOn();
    //
    // doNothing().when(spy).connectApi();
    //
    // when(this.ghApiMock.getRepository("octocat/Hello-World")).thenReturn(ghRepositoryMock);
    // doReturn(Optional.empty()).when(spy).getGHMilestone("v1.0.0");
    // doReturn(ghMilestoneCreated).when(spy).createGHMilestone("v1.0.0", GHMilestoneState.OPEN, Optional.of("description"), Optional.of(dueOn), Optional.empty());
    //
    // spy.execute();
    //
    // verify(this.ghActionsKitMock).getGitHubRepository();
    //
    // verify(spy).getInputTitle();
    // verify(spy).getInputState();
    // verify(spy).getInputDescription();
    // verify(spy).getInputDueOn();
    // verify(spy).connectApi();
    // verify(spy).getGHMilestone("v1.0.0");
    // verify(spy).createGHMilestone("v1.0.0", GHMilestoneState.OPEN, Optional.of("description"), Optional.of(dueOn), Optional.empty());
    //
    // verify(this.ghApiMock).getRepository("octocat/Hello-World");
    // verify(this.ghActionsKitMock).setOutput(OutputVars.NUMBER.key(), 123);
    // }
    //
    // /**
    // * Test method.
    // */
    // @Test
    // void whenExecuteCloseMilestoneExists_thenMilestoneClosed()
    // throws Exception {
    // var spy = spy(this.githubAction);
    //
    // var ghMilestoneExisting = Mockito.mock(GHMilestone.class);
    // when(ghMilestoneExisting.getNumber()).thenReturn(123);
    //
    // when(this.ghActionsKitMock.getGitHubRepository()).thenReturn("octocat/Hello-World");
    // doReturn("v1.0.0").when(spy).getInputTitle();
    // doReturn(InputMilestoneState.CLOSED).when(spy).getInputState();
    // doReturn(Optional.empty()).when(spy).getInputDescription();
    // doReturn(Optional.empty()).when(spy).getInputDueOn();
    //
    // doNothing().when(spy).connectApi();
    //
    // when(this.ghApiMock.getRepository("octocat/Hello-World")).thenReturn(ghRepositoryMock);
    // doReturn(Optional.of(ghMilestoneExisting)).when(spy).getGHMilestone("v1.0.0");
    // doReturn(ghMilestoneExisting).when(spy).createGHMilestone("v1.0.0", GHMilestoneState.CLOSED, Optional.empty(), Optional.empty(), Optional.of(ghMilestoneExisting));
    //
    // spy.execute();
    //
    // verify(this.ghActionsKitMock).getGitHubRepository();
    //
    // verify(spy).getInputTitle();
    // verify(spy).getInputState();
    // verify(spy).getInputDescription();
    // verify(spy).getInputDueOn();
    // verify(spy).connectApi();
    // verify(spy).getGHMilestone("v1.0.0");
    // verify(spy).createGHMilestone("v1.0.0", GHMilestoneState.CLOSED, Optional.empty(), Optional.empty(), Optional.of(ghMilestoneExisting));
    //
    // verify(this.ghApiMock).getRepository("octocat/Hello-World");
    // verify(this.ghActionsKitMock).setOutput(OutputVars.NUMBER.key(), 123);
    // }
    //
    // /**
    // * Test method.
    // */
    // @Test
    // void whenExecuteCloseMilestoneNotExists_thenMilestoneClosed()
    // throws Exception {
    // var spy = spy(this.githubAction);
    //
    // var ghMilestoneCreated = Mockito.mock(GHMilestone.class);
    // when(ghMilestoneCreated.getNumber()).thenReturn(123);
    //
    // when(this.ghActionsKitMock.getGitHubRepository()).thenReturn("octocat/Hello-World");
    // doReturn("v1.0.0").when(spy).getInputTitle();
    // doReturn(InputMilestoneState.CLOSED).when(spy).getInputState();
    // doReturn(Optional.empty()).when(spy).getInputDescription();
    // doReturn(Optional.empty()).when(spy).getInputDueOn();
    //
    // doNothing().when(spy).connectApi();
    //
    // when(this.ghApiMock.getRepository("octocat/Hello-World")).thenReturn(ghRepositoryMock);
    // doReturn(Optional.empty()).when(spy).getGHMilestone("v1.0.0");
    // doReturn(ghMilestoneCreated).when(spy).createGHMilestone("v1.0.0", GHMilestoneState.CLOSED, Optional.empty(), Optional.empty(), Optional.empty());
    //
    // spy.execute();
    //
    // verify(this.ghActionsKitMock).getGitHubRepository();
    //
    // verify(spy).getInputTitle();
    // verify(spy).getInputState();
    // verify(spy).getInputDescription();
    // verify(spy).getInputDueOn();
    // verify(spy).connectApi();
    // verify(spy).getGHMilestone("v1.0.0");
    // verify(spy).createGHMilestone("v1.0.0", GHMilestoneState.CLOSED, Optional.empty(), Optional.empty(), Optional.empty());
    //
    // verify(this.ghApiMock).getRepository("octocat/Hello-World");
    // verify(this.ghActionsKitMock).setOutput(OutputVars.NUMBER.key(), 123);
    // }
    //
    // /**
    // * Test method.
    // */
    // @Test
    // void whenExecuteDeleteMilestoneExists_thenMilestoneDeleted()
    // throws Exception {
    // var spy = spy(this.githubAction);
    //
    // var ghMilestoneExisting = Mockito.mock(GHMilestone.class);
    //
    // when(this.ghActionsKitMock.getGitHubRepository()).thenReturn("octocat/Hello-World");
    // doReturn("v1.0.0").when(spy).getInputTitle();
    // doReturn(InputMilestoneState.DELETED).when(spy).getInputState();
    // doReturn(Optional.empty()).when(spy).getInputDescription();
    // doReturn(Optional.empty()).when(spy).getInputDueOn();
    //
    // doNothing().when(spy).connectApi();
    //
    // when(this.ghApiMock.getRepository("octocat/Hello-World")).thenReturn(ghRepositoryMock);
    // doReturn(Optional.of(ghMilestoneExisting)).when(spy).getGHMilestone("v1.0.0");
    // doNothing().when(spy).deleteGHMilestone(Optional.of(ghMilestoneExisting));
    //
    // spy.execute();
    //
    // verify(this.ghActionsKitMock).getGitHubRepository();
    //
    // verify(spy).getInputTitle();
    // verify(spy).getInputState();
    // verify(spy).getInputDescription();
    // verify(spy).getInputDueOn();
    // verify(spy).connectApi();
    // verify(spy).getGHMilestone("v1.0.0");
    // verify(spy).deleteGHMilestone(Optional.of(ghMilestoneExisting));
    //
    // verify(this.ghApiMock).getRepository("octocat/Hello-World");
    // verify(this.ghActionsKitMock).setEmptyOutput(OutputVars.NUMBER.key());
    // }
    //
    // /**
    // * Test method.
    // */
    // @Test
    // void whenExecuteDeleteMilestoneNotExists_thenMilestoneDeleted()
    // throws Exception {
    // var spy = spy(this.githubAction);
    //
    // when(this.ghActionsKitMock.getGitHubRepository()).thenReturn("octocat/Hello-World");
    // doReturn("v1.0.0").when(spy).getInputTitle();
    // doReturn(InputMilestoneState.DELETED).when(spy).getInputState();
    // doReturn(Optional.empty()).when(spy).getInputDescription();
    // doReturn(Optional.empty()).when(spy).getInputDueOn();
    //
    // doNothing().when(spy).connectApi();
    //
    // when(this.ghApiMock.getRepository("octocat/Hello-World")).thenReturn(ghRepositoryMock);
    // doReturn(Optional.empty()).when(spy).getGHMilestone("v1.0.0");
    // doNothing().when(spy).deleteGHMilestone(Optional.empty());
    //
    // spy.execute();
    //
    // verify(this.ghActionsKitMock).getGitHubRepository();
    //
    // verify(spy).getInputTitle();
    // verify(spy).getInputState();
    // verify(spy).getInputDescription();
    // verify(spy).getInputDueOn();
    // verify(spy).connectApi();
    // verify(spy).getGHMilestone("v1.0.0");
    // verify(spy).deleteGHMilestone(Optional.empty());
    //
    // verify(this.ghApiMock).getRepository("octocat/Hello-World");
    // verify(this.ghActionsKitMock).setEmptyOutput(OutputVars.NUMBER.key());
    // }
    //
    /**
     * Test method.
     */
    @Test
    void whenConnectApi_thenVerifyOK()
        throws Exception {
        when(ghActionsKitMock.getRequiredEnv("GITHUB_TOKEN")).thenReturn("token");
        when(ghActionsKitMock.getGitHubApiUrl()).thenReturn("https://api.github.com");

        this.githubAction.connectApi();

        verify(ghActionsKitMock).getRequiredEnv("GITHUB_TOKEN");
        verify(ghActionsKitMock).getGitHubApiUrl();
        verify(ghActionsKitMock, times(2)).debug(Mockito.anyString());
        verify(ghApiMock).checkApiUrlValidity();
    }

    /**
     * Test method.
     */
    @Test
    void whenGetGHLabels_thenReturnLabels()
        throws Exception {
        var ghLabel1 = Mockito.mock(GHLabel.class);
        when(ghLabel1.getName()).thenReturn("label1");

        var ghLabel2 = Mockito.mock(GHLabel.class);
        when(ghLabel2.getName()).thenReturn("LABEL2");

        when(ghRepositoryMock.listLabels()).thenReturn(new LocalPagedIterable<>(List.of(ghLabel1, ghLabel2)));

        assertThat(this.githubAction.getGHLabels()).containsAllEntriesOf(Map.of("label1", ghLabel1, "label2", ghLabel2));

        verify(ghRepositoryMock).listLabels();
        verify(ghLabel1).getName();
        verify(ghLabel2).getName();
    }

    /**
     * Test method.
     */
    @Test
    void whenGetInputLabelsFromFile_thenGetLabels(@TempDir File tempDir)
        throws Exception {
        // Copy files
        var labelFiles = new ArrayList<>();
        for (String resource : Arrays.asList("labels.yaml", "labels.yml", "labels.json")) {
            var file = new File(tempDir, resource);
            try (var fos = new FileOutputStream(file)) {
                IOUtils.copy(getClass().getResourceAsStream("/labels/" + resource), fos);
            }
            labelFiles.add(file.getAbsolutePath());
        }

        // Read labels
        var fetchedLabels = this.githubAction.getInputLabels(labelFiles.toArray(String[]::new));
        //@formatter:off
        assertThat(fetchedLabels)
            .size().isEqualTo(8)
                .returnToMap()
            .containsKeys("label1", "label2", "label3", "label4", "label5", "label6", "label7", "label8");
        //@formatter:on

        // Assert conflicting labels take the latest file read (here json)
        assertThat(fetchedLabels.get("label1").getName()).isEqualTo("label1");
        assertThat(fetchedLabels.get("label1").getColor()).isEqualTo("000000");
        assertThat(fetchedLabels.get("label1").getDescription()).isEqualTo("label1 desc");

        // Assert label from .yaml loaded
        assertThat(fetchedLabels.get("label2").getName()).isEqualTo("label2");
        assertThat(fetchedLabels.get("label2").getColor()).isEqualTo("222222");
        assertThat(fetchedLabels.get("label2").getDescription()).isEqualTo("label2 desc");

        // Assert label from .yml loaded
        assertThat(fetchedLabels.get("label4").getName()).isEqualTo("label4");
        assertThat(fetchedLabels.get("label4").getColor()).isEqualTo("444444");
        assertThat(fetchedLabels.get("label4").getDescription()).isNull();
    }

    /**
     * Test method.
     */
    @Test
    void whenGetInputLabelsFromUrl_thenGetLabels()
        throws Exception {

        try {
            nginx.start();

            // Copy files
            var labelUrls = new ArrayList<>();
            for (String resource : Arrays.asList("labels.yaml", "labels.yml", "labels.json")) {
                labelUrls.add(nginx.getBaseUrl("http", 80) + "/" + resource);
            }

            // Read labels
            var fetchedLabels = this.githubAction.getInputLabels(labelUrls.toArray(String[]::new));
        //@formatter:off
        assertThat(fetchedLabels)
            .size().isEqualTo(8)
                .returnToMap()
            .containsKeys("label1", "label2", "label3", "label4", "label5", "label6", "label7", "label8");
        //@formatter:on

            // Assert conflicting labels take the latest file read (here json)
            assertThat(fetchedLabels.get("label1").getName()).isEqualTo("label1");
            assertThat(fetchedLabels.get("label1").getColor()).isEqualTo("000000");
            assertThat(fetchedLabels.get("label1").getDescription()).isEqualTo("label1 desc");

            // Assert label from .yaml loaded
            assertThat(fetchedLabels.get("label2").getName()).isEqualTo("label2");
            assertThat(fetchedLabels.get("label2").getColor()).isEqualTo("222222");
            assertThat(fetchedLabels.get("label2").getDescription()).isEqualTo("label2 desc");

            // Assert label from .yml loaded
            assertThat(fetchedLabels.get("label4").getName()).isEqualTo("label4");
            assertThat(fetchedLabels.get("label4").getColor()).isEqualTo("444444");
            assertThat(fetchedLabels.get("label4").getDescription()).isNull();
        } finally {
            nginx.stop();
        }
    }

    /**
     * Test method.
     */
    @Test
    void whenGetInputLabelsFromUnknownExtensionFile_thenThrowIllegalArgumentException()
        throws Exception {
        // Copy files
        assertThrows(IllegalArgumentException.class, () -> this.githubAction.getInputLabels(new String[] {"labels.txt"}));
    }

    /**
     * Test method.
     */
    @Test
    void whenGetInputLabelsNull_thenThrowNullPointerException()
        throws Exception {
        // Copy files
        assertThrows(NullPointerException.class, () -> this.githubAction.getInputLabels(null));
    }

    /**
     * Test method.
     */
    @Test
    void whenGetInputStreamNull_thenThrowNullPointerException()
        throws Exception {
        // Copy files
        assertThrows(NullPointerException.class, () -> this.githubAction.getInputStream(null));
    }

    /**
     * Test method.
     */
    @Test
    void whenCreateLabels_thenCreateGhLabels()
        throws Exception {
        var label1 = new LabelDTO("label1", "000000");
        var label2 = new LabelDTO("label2", "000000");

        assertDoesNotThrow(() -> {
            this.githubAction.createLabels(List.of(label1, label2));
        });

        verify(ghActionsKitMock, times(2)).notice(Mockito.anyString());
        verify(ghRepositoryMock).createLabel("label1", "000000", null);
        verify(ghRepositoryMock).createLabel("label2", "000000", null);
    }

    /**
     * Test method.
     */
    @Test
    void whenCreateLabelsNull_thenThrowNullPointerException()
        throws Exception {
        assertThrows(NullPointerException.class, () -> this.githubAction.createLabels(null));
    }

    /**
     * Test method.
     */
    @Test
    void whenUpdateLabels_thenUpdateGhLabels()
        throws Exception {
        var label1 = new LabelDTO("label1", "000000");
        var label2 = new LabelDTO("label2", "111111", "some desc");

        var mockUpdater = mock(Updater.class);
        when(mockUpdater.name(anyString())).thenReturn(mockUpdater);
        when(mockUpdater.color(anyString())).thenReturn(mockUpdater);
        when(mockUpdater.description(Mockito.any())).thenReturn(mockUpdater);
        when(mockUpdater.done()).thenReturn(null);

        var ghLabel1 = mock(GHLabel.class);
        when(ghLabel1.update()).thenReturn(mockUpdater);
        var ghLabel2 = mock(GHLabel.class);
        when(ghLabel2.update()).thenReturn(mockUpdater);

        assertDoesNotThrow(() -> {
            this.githubAction.updateLabels(Map.of(label1, ghLabel1, label2, ghLabel2));
        });

        verify(ghActionsKitMock, times(2)).notice(Mockito.anyString());
        verify(ghLabel1).update();
        verify(ghLabel2).update();

        verify(mockUpdater).name("label1");
        verify(mockUpdater).color("000000");
        verify(mockUpdater).description(null);
        verify(mockUpdater).name("label2");
        verify(mockUpdater).color("111111");
        verify(mockUpdater).description("some desc");
        verify(mockUpdater, times(2)).done();
    }

    /**
     * Test method.
     */
    @Test
    void whenUpdateLabelsNull_thenThrowNullPointerException()
        throws Exception {
        assertThrows(NullPointerException.class, () -> this.githubAction.updateLabels(null));
    }

    /**
     * Test method.
     */
    @Test
    void whenDeleteLabels_thenDeleteGhLabels()
        throws Exception {
        var ghLabel1 = Mockito.mock(GHLabel.class);
        var ghLabel2 = Mockito.mock(GHLabel.class);

        assertDoesNotThrow(() -> {
            this.githubAction.deleteLabels(List.of(ghLabel1, ghLabel2));
        });

        verify(ghActionsKitMock, times(2)).notice(Mockito.anyString());
        verify(ghLabel1).delete();
        verify(ghLabel2).delete();
    }

    /**
     * Test method.
     */
    @Test
    void whenDeleteLabelsNull_thenThrowNullPointerException()
        throws Exception {
        assertThrows(NullPointerException.class, () -> this.githubAction.deleteLabels(null));
    }

    /**
     * Test method.
     */
    @Test
    void whenCreateLabelDTONull_thenThrowNullPointerException()
        throws Exception {
        assertThrows(NullPointerException.class, () -> new LabelDTO(null, "000000"));
        assertThrows(NullPointerException.class, () -> new LabelDTO("label1", null));
        assertThrows(NullPointerException.class, () -> new LabelDTO(null, "000000", "some desc"));
        assertThrows(NullPointerException.class, () -> new LabelDTO("label1", null, "some desc"));
    }

    /**
     * Test method.
     */
    @Test
    @SuppressWarnings({"java:S5838", "java:S5845", "java:S5863"})
    void whenComparingLabelDTO_thenReturnValidValue()
        throws Exception {
        var label1 = new LabelDTO("LABEL1", "000000");
        var label2 = new LabelDTO("label2", "000000");
        var label3 = new LabelDTO("Label3", "000000");
        var label4 = new LabelDTO("label1", "000000", "label1 desc");
        assertThat(label1.compareTo(label2)).isNegative();
        assertThat(label1).isEqualTo(label1).isNotEqualTo(label2).isNotEqualTo("Hello").isEqualTo(label4);
        assertThat(label2.compareTo(label3)).isNegative();
        assertThat(label1.compareTo(label4)).isZero();
        assertThat(label1).isEqualTo(label4);
    }
}
