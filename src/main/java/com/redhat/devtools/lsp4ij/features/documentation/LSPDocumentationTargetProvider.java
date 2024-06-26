/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.features.documentation;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.backend.documentation.DocumentationTarget;
import com.intellij.platform.backend.documentation.DocumentationTargetProvider;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LSPFileSupport;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.isDoneNormally;
import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.waitUntilDone;

/**
 * LSP {@link DocumentationTargetProvider} implementation used to consume
 * LSP 'textDocument/hover' request and returns alist of {@link LSPDocumentationTarget}
 * per language server.
 */
public class LSPDocumentationTargetProvider implements DocumentationTargetProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(LSPDocumentationTargetProvider.class);

    @Override
    public @NotNull List<? extends @NotNull DocumentationTarget> documentationTargets(@NotNull PsiFile psiFile, int offset) {
        Project project = psiFile.getProject();
        if (project.isDisposed() || DumbService.isDumb(project)) {
            return Collections.emptyList();
        }
        VirtualFile file = LSPIJUtils.getFile(psiFile);
        if (file == null) {
            return Collections.emptyList();
        }
        final Document document = LSPIJUtils.getDocument(file);
        if (document == null) {
            return Collections.emptyList();
        }
        LSPHoverSupport hoverSupport = LSPFileSupport.getSupport(psiFile).getHoverSupport();
        CompletableFuture<List<Hover>> hoverFuture = hoverSupport.getHover(offset, document);

        try {
            waitUntilDone(hoverFuture, psiFile);
        } catch (ProcessCanceledException e) {
            // cancel the LSP requests textDocument/hover
            hoverSupport.cancel();
        } catch (CancellationException e) {
            // cancel the LSP requests textDocument/hover
            hoverSupport.cancel();
        } catch (ExecutionException e) {
            LOGGER.error("Error while consuming LSP 'textDocument/hover' request", e);
        }

        if (isDoneNormally(hoverFuture)) {
            // textDocument/hover has been collected correctly
            List<Hover> hovers = hoverFuture.getNow(null);
            if (hovers != null) {
                Editor editor = LSPIJUtils.editorForElement(psiFile);
                return hovers
                        .stream()
                        .map(hover -> toDocumentTarget(hover, document, editor))
                        .filter(Objects::nonNull)
                        .toList();
            }
        }

        return Collections.emptyList();
    }

    @Nullable
    private static DocumentationTarget toDocumentTarget(@Nullable Hover hover,
                                                        @NotNull Document document,
                                                        @Nullable Editor editor) {
        if (hover == null) {
            return null;
        }
        String presentationText = getPresentationText(hover, document);
        return new LSPDocumentationTarget(hover, presentationText, editor);
    }

    private static String getPresentationText(@NotNull Hover hover, @NotNull Document document) {
        Range range = hover.getRange();
        if (range != null) {
            TextRange textRange = LSPIJUtils.toTextRange(range, document);
            return textRange != null ? document.getText(textRange) : null;
        }
        return null;
    }
}
