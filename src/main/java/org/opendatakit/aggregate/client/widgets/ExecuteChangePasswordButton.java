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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PasswordTextBox;
import java.security.NoSuchAlgorithmException;
import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.permissions.CredentialsInfoBuilder;
import org.opendatakit.aggregate.client.popups.ChangePasswordPopup;
import org.opendatakit.common.security.client.CredentialsInfo;
import org.opendatakit.common.security.client.RealmSecurityInfo;
import org.opendatakit.common.security.client.UserSecurityInfo;

/**
 * Uses whatever the secure channel is (https: if available; http: if not) and
 * GWT RequestBuilder to POST back to ODK Aggregate to change a user's password.
 * The password is sent as a hash back to the server, so even if it is sent over
 * http: (which happens if the server does not have SSL configured), it would
 * take a while to compromise the password.
 *
 * @author mitchellsundt@gmail.com
 */
public final class ExecuteChangePasswordButton extends AggregateButton implements ClickHandler {

  private static final String BUTTON_TXT = "<img src=\"images/green_right_arrow.png\" /> Changer le mot de passe";
  private static final String TOOLTIP_TXT = "Changer le mot de passe de l'utilisateur";
  private static final String HELP_BALLOON_TXT = "Changer le mot de passe de l'utilisateur lors de la connexion à Peogo Survey.";

  private static int jsonRequestId = 0;

  private ChangePasswordPopup popup;
  private String baseUrl;

  public ExecuteChangePasswordButton(ChangePasswordPopup popup) {
    super(BUTTON_TXT, TOOLTIP_TXT, HELP_BALLOON_TXT);
    this.popup = popup;
  }

  public native static void getJson(int requestId, String url, ExecuteChangePasswordButton handler) /*-{
    var callback = "callback" + requestId;

    var script = document.createElement("script");
    script.setAttribute("src", url + callback);
    script.setAttribute("type", "text/javascript");

    window[callback] = function (jsonObj) {
      window[callback + "done"] = true;
      handler.@org.opendatakit.aggregate.client.widgets.ExecuteChangePasswordButton::handleJsonResponse(Ljava/lang/String;Ljava/lang/String;)(jsonObj.username, jsonObj.status);
    }

    // JSON change password has 15-second timeout
    setTimeout(
        function () {
          if (!window[callback + "done"]) {
            handler.@org.opendatakit.aggregate.client.widgets.ExecuteChangePasswordButton::handleJsonResponse(Ljava/lang/String;Ljava/lang/String;)(null, null);
          }

          // cleanup
          document.body.removeChild(script);
          delete window[callback];
          delete window[callback + "done"];
        }, 15000);

    document.body.appendChild(script);
  }-*/;

  @Override
  public void onClick(ClickEvent event) {
    super.onClick(event);

    PasswordTextBox password1 = popup.getPassword1();
    PasswordTextBox password2 = popup.getPassword2();
    UserSecurityInfo userInfo = popup.getUser();
    RealmSecurityInfo realmInfo = AggregateUI.getUI().getRealmInfo();

    String pw1 = password1.getText();
    String pw2 = password2.getText();
    if (pw1 == null || pw2 == null || pw1.length() == 0) {
      Window.alert("Le mot de passe ne peut pas être vide");
    } else if (pw1.equals(pw2)) {
      if (realmInfo == null || userInfo == null) {
        Window.alert("Impossible d'obtenir les informations requises du serveur");
      } else {
        CredentialsInfo credential;
        try {
          credential = CredentialsInfoBuilder.build(userInfo.getUsername(), realmInfo, pw1);
        } catch (NoSuchAlgorithmException e) {
          Window.alert("Impossible de créer un hachage des informations d'identification");
          return;
        }

        baseUrl = realmInfo.getChangeUserPasswordURL();

        // Construct a JSOP request
        String parameters = credential.getRequestParameters();
        String url = baseUrl + "?" + parameters + "&callback=";
        getJson(jsonRequestId++, url, this);
      }
    } else {
      Window.alert("Le mot de passe ne correspond pas. Veuillez ressaisir le mot de passe.");
    }
  }

  public void handleJsonResponse(String username, String status) {
    if (username == null) {
      Window.alert("Demande de changement de mot de passe JSON à " + baseUrl + " échoué");
    } else {
      // process response...
      if (!(status != null && "OK".equals(status))) {
        Window.alert("Changer la demande de mot de passe "
            + ((username == null) ? "" : ("pour " + username + " ")) + "échoué.\n"
            + "Demande de changement de mot de passe JSON à\n   " + baseUrl + "\nretourné: " + status);
      }
    }
    popup.hide();
    AggregateUI.getUI().forceUpdateNotSecureInfo();
  }

}
