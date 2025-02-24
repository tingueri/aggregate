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
import static org.opendatakit.common.security.common.GrantedAuthorityName.ROLE_DATA_OWNER;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.SecureGWT;

public final class DownloadableCheckBox extends AggregateCheckBox implements ValueChangeHandler<Boolean> {

  private static final String TOOLTIP_TXT = "Autoriser ou interdire le téléchargement du formulaire";
  private static final String HELP_BALLOON_TXT = "Cochez cette case si vous souhaitez que votre formulaire soit téléchargeable. Sinon laisser non cochée.";

  private final String formId;

  public DownloadableCheckBox(String formId, Boolean downloadable) {
    super(null, false, TOOLTIP_TXT, HELP_BALLOON_TXT);
    this.formId = formId;
    setValue(downloadable);
    boolean enabled = AggregateUI.getUI().getUserInfo().getGrantedAuthorities().contains(ROLE_DATA_OWNER);
    setEnabled(enabled);
  }

  @Override
  public void onValueChange(ValueChangeEvent<Boolean> event) {
    super.onValueChange(event);
    secureRequest(
        SecureGWT.getFormAdminService(),
        (rpc, sessionCookie, cb) -> rpc.setFormDownloadable(formId, event.getValue(), cb),
        this::onSuccess,
        this::onError
    );
  }

  private void onError(Throwable cause) {
    AggregateUI.getUI().reportError(cause);
  }

  private void onSuccess() {
    AggregateUI.getUI().clearError();
  }
}