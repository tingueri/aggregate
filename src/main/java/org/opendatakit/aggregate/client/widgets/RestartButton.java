/*
 * Copyright (C) 2011 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.aggregate.client.widgets;

import static org.opendatakit.aggregate.client.security.SecurityUtils.secureRequest;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.SecureGWT;
import org.opendatakit.aggregate.client.externalserv.ExternServSummary;

/**
 * Restarts Publisher because of failures
 */
public final class RestartButton extends AggregateButton implements ClickHandler {

  public static final Runnable NO_OP_RUNNABLE = () -> {
  };
  private static final String BUTTON_BAD_CREDENTIAL_TXT = "<b><img src=\"images/green_right_arrow.png\" /> Redémarrer Publisher - Les informations d'identification étaient BAD";

  private static final String TOOLTIP_BAD_CREDENTIAL_TEXT = "Échec de publication en raison de mauvaises informations d'identification - cliquez sur Redémarrer l'éditeur";
  private static final String HELP_BALLOON_BAD_CREDENTIAL_TXT = "Le service externe échouait ou les informations d'identification étaient mauvaises. Cliquez sur pour redémarrer l'éditeur.";
  private static final String BUTTON_FAILURE_TXT = "<b><img src=\"images/green_right_arrow.png\" /> Redémarrer Publisher - Échec";
  private static final String TOOLTIP_FAILURE_TEXT = "Échec de publication en raison d'un échec répété - cliquez pour redémarrer le serveur de publication";
  private static final String HELP_BALLOON_FAILURE_TXT = "Le service externe échouait. Cliquez sur pour redémarrer l'éditeur.";
  private static final String BUTTON_PAUSED_TXT = "<b><img src=\"images/green_right_arrow.png\" /> Redémarrer Publisher - En pause";
  private static final String TOOLTIP_PAUSED_TEXT = "Publication suspendue en raison d'une erreur du service - cliquez pour redémarrer le serveur de publication";
  private static final String HELP_BALLOON_PAUSED_TXT = "Le service externe a échoué (réessayera dans quelques minutes). Cliquez pour redémarrer l'éditeur.";
  private final ExternServSummary publisher;

  public RestartButton(ExternServSummary publisher, Circumstance credentialFailure) {
    super((credentialFailure == Circumstance.CREDENTIALS) ? BUTTON_BAD_CREDENTIAL_TXT :
            ((credentialFailure == Circumstance.CREDENTIALS) ? BUTTON_FAILURE_TXT : BUTTON_PAUSED_TXT),
        (credentialFailure == Circumstance.CREDENTIALS) ? TOOLTIP_BAD_CREDENTIAL_TEXT :
            ((credentialFailure == Circumstance.CREDENTIALS) ? TOOLTIP_FAILURE_TEXT : TOOLTIP_PAUSED_TEXT),
        (credentialFailure == Circumstance.CREDENTIALS) ? HELP_BALLOON_BAD_CREDENTIAL_TXT :
            ((credentialFailure == Circumstance.CREDENTIALS) ? HELP_BALLOON_FAILURE_TXT : HELP_BALLOON_PAUSED_TXT));
    this.publisher = publisher;
    addStyleDependentName("negative");
  }

  @Override
  public void onClick(ClickEvent event) {
    super.onClick(event);

    secureRequest(
        SecureGWT.getServicesAdminService(),
        (rpc, sc, cb) -> rpc.restartPublisher(publisher.getUri(), cb),
        NO_OP_RUNNABLE,
        this::onFailure
    );
  }

  private void onFailure(Throwable cause) {
    AggregateUI.getUI().reportError(cause);
  }

  public enum Circumstance {CREDENTIALS, ABANDONED, PAUSED}
}
