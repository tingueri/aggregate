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

public class MarkSubmissionCompleteButton extends AggregateButton implements ClickHandler {

  private static final String BUTTON_TXT = "Mark Complete";
  private static final String TOOLTIP_TEXT = "Marquer la soumission comme COMPLETE";
  private static final String HELP_BALLOON_TXT = "Marquez la soumission comme complète afin qu’elle apparaisse dans l’UI. Une soumission peut être incomplète en raison de problèmes de transmission ou d'une autre raison inconnue..";

  private final String submissionKeyAsString;

  public MarkSubmissionCompleteButton(String submissionKeyAsString) {
    super(BUTTON_TXT, TOOLTIP_TEXT, HELP_BALLOON_TXT);
    this.submissionKeyAsString = submissionKeyAsString;
    addStyleDependentName("negative");
  }

  @Override
  public void onClick(ClickEvent event) {
    super.onClick(event);
    secureRequest(
        SecureGWT.getFormAdminService(),
        (rpc, sessionCookie, cb) -> rpc.markSubmissionAsComplete(submissionKeyAsString, cb),
        this::onSuccess,
        this::onError
    );
  }

  private void onError(Throwable cause) {
    AggregateUI.getUI().reportError(cause);
  }

  private void onSuccess() {
    AggregateUI.getUI().clearError();
    AggregateUI.getUI().getTimer().refreshNow();
  }
}
