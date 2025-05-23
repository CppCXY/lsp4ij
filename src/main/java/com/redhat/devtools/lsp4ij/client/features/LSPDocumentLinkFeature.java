/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.client.features;

import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.server.capabilities.DocumentLinkCapabilityRegistry;
import org.eclipse.lsp4j.ServerCapabilities;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * LSP documentLink feature.
 */
@ApiStatus.Experimental
public class LSPDocumentLinkFeature extends AbstractLSPDocumentFeature {

    private DocumentLinkCapabilityRegistry documentLinkCapabilityRegistry;

    @Override
    public boolean isSupported(@NotNull PsiFile file) {
        return isDocumentLinkSupported(file);
    }

    /**
     * Returns true if the file associated with a language server can support codelens and false otherwise.
     *
     * @param file the file.
     * @return true if the file associated with a language server can support codelens and false otherwise.
     */
    public boolean isDocumentLinkSupported(@NotNull PsiFile file) {
        return getDocumentLinkCapabilityRegistry().isDocumentLinkSupported(file);
    }

    public DocumentLinkCapabilityRegistry getDocumentLinkCapabilityRegistry() {
        if (documentLinkCapabilityRegistry == null) {
            initDocumentLinkCapabilityRegistry();
        }
        return documentLinkCapabilityRegistry;
    }

    private synchronized void initDocumentLinkCapabilityRegistry() {
        if (documentLinkCapabilityRegistry != null) {
            return;
        }
        var clientFeatures = getClientFeatures();
        documentLinkCapabilityRegistry = new DocumentLinkCapabilityRegistry(clientFeatures);
        documentLinkCapabilityRegistry.setServerCapabilities(clientFeatures.getServerWrapper().getServerCapabilitiesSync());
    }

    @Override
    public void setServerCapabilities(@Nullable ServerCapabilities serverCapabilities) {
        if (documentLinkCapabilityRegistry != null) {
            documentLinkCapabilityRegistry.setServerCapabilities(serverCapabilities);
        }
    }
}
