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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Sets;

import me.julb.sdk.github.actions.kit.GitHubActionsKit;
import me.julb.sdk.github.actions.spi.GitHubActionProvider;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;

/**
 * The action to manage labels. <br>
 * @author Julb.
 */
public class ManageLabelGitHubAction implements GitHubActionProvider {

    /**
     * The GitHub action kit.
     */
    @Setter(AccessLevel.PACKAGE)
    private GitHubActionsKit ghActionsKit = GitHubActionsKit.INSTANCE;

    /**
     * The GitHub API.
     */
    @Setter(AccessLevel.PACKAGE)
    private GitHub ghApi;

    /**
     * The GitHub repository.
     */
    @Setter(AccessLevel.PACKAGE)
    private GHRepository ghRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        try {
            // Get inputs
            var labelSourcesFrom = getInputFrom();
            var labelSkipDeletion = getInputSkipDelete();

            // Trace parameters
            ghActionsKit.debug(String.format(
                    "parameters: [from: %s, skipDeletion: %s]", Arrays.toString(labelSourcesFrom), labelSkipDeletion));

            // Read GitHub repository.
            connectApi();

            // Retrieve repository
            ghRepository = ghApi.getRepository(ghActionsKit.getGitHubRepository());

            // Get label from sources.
            var labelsToSynchronize = getInputLabels(labelSourcesFrom);

            // Get existing labels in repository.
            var existingGHLabels = getGHLabels();

            // Get labels to create
            var labelNamesToCreate = Sets.difference(labelsToSynchronize.keySet(), existingGHLabels.keySet());
            var labelsToCreate = new TreeSet<LabelDTO>();
            for (String labelNameToCreate : labelNamesToCreate) {
                labelsToCreate.add(labelsToSynchronize.get(labelNameToCreate));
            }
            createLabels(labelsToCreate);

            // Get labels to update
            var labelNamesToUpdate = Sets.intersection(labelsToSynchronize.keySet(), existingGHLabels.keySet());
            var labelsToUpdate = new TreeMap<LabelDTO, GHLabel>();
            for (String labelNameToUpdate : labelNamesToUpdate) {
                labelsToUpdate.put(labelsToSynchronize.get(labelNameToUpdate), existingGHLabels.get(labelNameToUpdate));
            }
            updateLabels(labelsToUpdate);

            // Get labels to delete
            if (!labelSkipDeletion) {
                var labelNamesToDelete = Sets.difference(existingGHLabels.keySet(), labelsToSynchronize.keySet());
                var labelsToDelete = new ArrayList<GHLabel>();
                for (String labelNameToDelete : labelNamesToDelete) {
                    labelsToDelete.add(existingGHLabels.get(labelNameToDelete));
                }
                deleteLabels(labelsToDelete);
            }

        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    // ------------------------------------------ Utility methods.

    /**
     * Gets the "from" input.
     * @return the "from" input.
     */
    String[] getInputFrom() {
        return ghActionsKit.getRequiredMultilineInput("from");
    }

    /**
     * Gets the "skip_delete" input.
     * @return the "skip_delete" input.
     */
    boolean getInputSkipDelete() {
        return ghActionsKit.getBooleanInput("skip_delete").orElse(Boolean.FALSE);
    }

    /**
     * Connects to GitHub API.
     * @throws IOException if an error occurs.
     */
    void connectApi() throws IOException {
        ghActionsKit.debug("github api url connection: check.");

        // Get token
        var githubToken = ghActionsKit.getRequiredEnv("GITHUB_TOKEN");

        // @formatter:off
        ghApi = Optional.ofNullable(ghApi)
                .orElse(new GitHubBuilder()
                        .withEndpoint(ghActionsKit.getGitHubApiUrl())
                        .withOAuthToken(githubToken)
                        .build());
        ghApi.checkApiUrlValidity();
        ghActionsKit.debug("github api url connection: ok.");
        // @formatter:on
    }

    /**
     * Gets all {@link GHLabel} present in the repository.
     * @return all {@link GHLabel} present in the repository.
     * @throws IOException if an error occurs.
     */
    Map<String, LabelDTO> getInputLabels(@NonNull String[] labelSources) throws IOException {
        // Get JSON and YAML deserializer.
        var jsonObjectMapper = new ObjectMapper();
        var yamlObjectMapper = new ObjectMapper(new YAMLFactory());

        Map<String, LabelDTO> map = new TreeMap<>();

        for (String labelSource : labelSources) {
            ghActionsKit.notice(String.format("processing source '%s'.", labelSource));

            // Determine object mapper.
            ObjectMapper objectMapper;
            var extension = FilenameUtils.getExtension(labelSource);
            if (extension.equalsIgnoreCase("yaml") || extension.equalsIgnoreCase("yml")) {
                objectMapper = yamlObjectMapper;
            } else if (extension.equalsIgnoreCase("json")) {
                objectMapper = jsonObjectMapper;
            } else {
                throw new IllegalArgumentException(labelSource);
            }

            // Get input stream.
            try (var is = getInputStream(labelSource)) {
                // Read and get labels.
                var labels = objectMapper.readValue(is, new TypeReference<ArrayList<LabelDTO>>() {});
                labels.forEach(label -> map.put(label.nameLowerCase(), label));
                ghActionsKit.notice(String.format("%d labels fetched.", labels.size()));
            }
        }

        return map;
    }

    /**
     * Gets the input stream according to the given source.
     * @param labelSource the label source.
     * @return the stream to consume that source.
     * @throws IOException if an error occurs.
     */
    InputStream getInputStream(@NonNull String labelSource) throws IOException {
        if (Pattern.matches("^[hH][tT][tT][pP][sS]?://.*", labelSource)) {
            return new URL(labelSource).openStream();
        } else {
            return new FileInputStream(labelSource);
        }
    }

    /**
     * Gets all {@link GHLabel} present in the repository.
     * @return all {@link GHLabel} present in the repository.
     * @throws IOException if an error occurs.
     */
    Map<String, GHLabel> getGHLabels() throws IOException {

        Map<String, GHLabel> map = new TreeMap<>();
        for (GHLabel ghLabel : ghRepository.listLabels()) {
            map.put(ghLabel.getName().toLowerCase(), ghLabel);
        }

        return map;
    }

    /**
     * Create the given labels in the repository.
     * @param labelsToCreate the labels to create.
     * @throws IOException if an error occurs.
     */
    void createLabels(@NonNull Collection<LabelDTO> labelsToCreate) throws IOException {
        for (LabelDTO label : labelsToCreate) {
            this.ghActionsKit.notice(String.format("creating label '%s'", label.getName()));
            ghRepository.createLabel(label.getName(), label.getColor(), label.getDescription());
        }
    }

    /**
     * Updates the given labels in the repository.
     * @param labelsToUpdate the labels to update.
     * @throws IOException if an error occurs.
     */
    void updateLabels(@NonNull Map<LabelDTO, GHLabel> labelsToUpdate) throws IOException {
        for (Map.Entry<LabelDTO, GHLabel> entry : labelsToUpdate.entrySet()) {
            var sourceLabel = entry.getKey();
            var ghLabel = entry.getValue();

            // Trace
            this.ghActionsKit.notice(String.format("updating label '%s'", ghLabel.getName()));

            // @formatter:off
            ghLabel.update()
                    .name(sourceLabel.getName())
                    .color(sourceLabel.getColor())
                    .description(sourceLabel.getDescription())
                    .done();
            // @formatter:on
        }
    }

    /**
     * Deletes the given labels from the repository.
     * @param labelsToDelete the labels to create.
     * @throws IOException if an error occurs.
     */
    void deleteLabels(@NonNull Collection<GHLabel> labelsToDelete) throws IOException {
        for (GHLabel label : labelsToDelete) {
            this.ghActionsKit.notice(String.format("deleting label '%s'", label.getName()));
            label.delete();
        }
    }
}
