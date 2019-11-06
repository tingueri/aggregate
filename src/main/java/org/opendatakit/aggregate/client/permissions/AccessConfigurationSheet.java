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

package org.opendatakit.aggregate.client.permissions;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.PermissionsSubTab;
import org.opendatakit.aggregate.client.SecureGWT;
import org.opendatakit.aggregate.client.popups.ChangePasswordPopup;
import org.opendatakit.aggregate.client.popups.ConfirmUserDeletePopup;
import org.opendatakit.aggregate.client.security.SecurityUtils;
import org.opendatakit.aggregate.client.widgets.UploadUsersAndPermsServletPopupButton;
import org.opendatakit.aggregate.constants.common.UIConsts;
import org.opendatakit.common.security.client.UserSecurityInfo;
import org.opendatakit.common.security.client.UserSecurityInfo.UserType;
import org.opendatakit.common.security.common.EmailParser;
import org.opendatakit.common.security.common.GrantedAuthorityName;
import org.opendatakit.common.web.client.BooleanValidationPredicate;
import org.opendatakit.common.web.client.StringValidationPredicate;
import org.opendatakit.common.web.client.UIEnabledActionCell;
import org.opendatakit.common.web.client.UIEnabledActionColumn;
import org.opendatakit.common.web.client.UIEnabledPredicate;
import org.opendatakit.common.web.client.UIEnabledValidatingCheckboxColumn;
import org.opendatakit.common.web.client.UIEnabledValidatingTextInputColumn;
import org.opendatakit.common.web.client.UIVisiblePredicate;

public class AccessConfigurationSheet extends Composite {

  private static TemporaryAccessConfigurationSheetUiBinder uiBinder = GWT
      .create(TemporaryAccessConfigurationSheetUiBinder.class);

  private final ListDataProvider<UserSecurityInfo> dataProvider = new ListDataProvider<UserSecurityInfo>();
  private final ListHandler<UserSecurityInfo> columnSortHandler = new ListHandler<UserSecurityInfo>(
      dataProvider.getList());
  @UiField
  TextArea addedUsers;
  @UiField
  UploadUsersAndPermsServletPopupButton uploadCsv;
  @UiField
  Anchor downloadCsv;
  @UiField
  Button addNow;
  @UiField
  CellTable<UserSecurityInfo> userTable;
  @UiField
  CheckBox anonymousAttachmentViewers;
  @UiField
  Button button;
  private boolean anonymousAttachmentBoolean = false;
  private PermissionsSubTab permissionsTab;
  private boolean changesHappened = false;
  private GroupMembershipColumn formsAdmin;
  private GroupMembershipColumn siteAdmin;

  public AccessConfigurationSheet(PermissionsSubTab permissionsTab) {
    this.permissionsTab = permissionsTab;
    initWidget(uiBinder.createAndBindUi(this));
    sinkEvents(Event.ONCHANGE | Event.ONCLICK);

    downloadCsv.setHref(UIConsts.GET_USERS_AND_PERMS_CSV_SERVLET_ADDR);

    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<img src=\"images/red_x.png\" />");
    UIEnabledActionColumn<UserSecurityInfo> deleteMe = new UIEnabledActionColumn<UserSecurityInfo>(
        sb.toSafeHtml(), null, new EnableNotAnonymousOrSuperUserPredicate(),
        new DeleteActionCallback());
    userTable.addColumn(deleteMe, "");

    // Username
    UsernameTextColumn username = new UsernameTextColumn();
    userTable.addColumn(username, "Code Utilisateur");

    // Full Name
    FullNameTextColumn fullname = new FullNameTextColumn();
    userTable.addColumn(fullname, "Nom complet");

    // Change Password
    UIEnabledActionColumn<UserSecurityInfo> changePassword = new UIEnabledActionColumn<UserSecurityInfo>(
        "Changer Mot passe", new EnableLocalAccountPredicate(), new ChangePasswordActionCallback());
    userTable.addColumn(changePassword, "");

    GroupMembershipColumn dc = new GroupMembershipColumn(GrantedAuthorityName.GROUP_DATA_COLLECTORS);
    userTable.addColumn(dc, GrantedAuthorityName.GROUP_DATA_COLLECTORS.getDisplayText());

    GroupMembershipColumn dv = new GroupMembershipColumn(GrantedAuthorityName.GROUP_DATA_VIEWERS);
    userTable.addColumn(dv, GrantedAuthorityName.GROUP_DATA_VIEWERS.getDisplayText());

    formsAdmin = new GroupMembershipColumn(GrantedAuthorityName.GROUP_FORM_MANAGERS);
    userTable.addColumn(formsAdmin, GrantedAuthorityName.GROUP_FORM_MANAGERS.getDisplayText());

    columnSortHandler.setComparator(username, username.getComparator());
    columnSortHandler.setComparator(fullname, fullname.getComparator());
    columnSortHandler.setComparator(dc, dc.getComparator());
    columnSortHandler.setComparator(dv, dv.getComparator());
    columnSortHandler.setComparator(formsAdmin, formsAdmin.getComparator());

    siteAdmin = new GroupMembershipColumn(GrantedAuthorityName.GROUP_SITE_ADMINS);
    userTable.addColumn(siteAdmin, GrantedAuthorityName.GROUP_SITE_ADMINS.getDisplayText());
    columnSortHandler.setComparator(siteAdmin, siteAdmin.getComparator());

    dataProvider.addDataDisplay(userTable);

    userTable.addColumnSortHandler(columnSortHandler);
  }

  public boolean isUiOutOfSyncWithServer() {
    return changesHappened;
  }

  private void uiInSyncWithServer() {
    changesHappened = false;
  }

  private void uiOutOfSyncWithServer() {
    changesHappened = true;
  }

  public void deleteUser(UserSecurityInfo user) {
    dataProvider.getList().remove(user);
    updateUsersOnServer();
  }

  public void updateUsersOnServer() {
    final ArrayList<GrantedAuthorityName> allGroups = new ArrayList<GrantedAuthorityName>();
    allGroups.add(GrantedAuthorityName.GROUP_SITE_ADMINS);
    allGroups.add(GrantedAuthorityName.GROUP_FORM_MANAGERS);
    allGroups.add(GrantedAuthorityName.GROUP_DATA_VIEWERS);
    allGroups.add(GrantedAuthorityName.GROUP_DATA_COLLECTORS);
    allGroups.add(GrantedAuthorityName.USER_IS_ANONYMOUS);

    ArrayList<UserSecurityInfo> users = new ArrayList<UserSecurityInfo>();
    users.addAll(dataProvider.getList());
    for (UserSecurityInfo i : users) {
      if (i.getType() == UserType.ANONYMOUS) {
        if (anonymousAttachmentBoolean) {
          i.getAssignedUserGroups().add(GrantedAuthorityName.ROLE_ATTACHMENT_VIEWER);
        } else {
          i.getAssignedUserGroups().remove(GrantedAuthorityName.ROLE_ATTACHMENT_VIEWER);
        }
        break;
      } else {
        if (i.getUsername() == null) {
          // don't allow Google users to be data collectors
          i.getAssignedUserGroups().remove(GrantedAuthorityName.GROUP_DATA_COLLECTORS);
        }
      }
    }
    SecurityUtils.secureRequest(
        SecureGWT.getSecurityAdminService(),
        (rpc, sessionCookie, callback) -> rpc.setUsersAndGrantedAuthorities(sessionCookie, users, allGroups, callback),
        () -> SecureGWT.getSecurityAdminService().getAllUsers(true, new UpdateUserDisplay()),
        cause -> AggregateUI.getUI().reportError("Incomplete security update: ", cause)
    );

  }

  @Override
  public void setVisible(boolean isVisible) {
    super.setVisible(isVisible);
    if (isVisible) {
      SecureGWT.getSecurityAdminService().getAllUsers(true, new UpdateUserDisplay());
    }
  }

  @UiHandler("anonymousAttachmentViewers")
  void onAnonAttachmentViewerChange(ValueChangeEvent<Boolean> event) {
    anonymousAttachmentBoolean = event.getValue();
    uiOutOfSyncWithServer();
  }

  @UiHandler("uploadCsv")
  void onUploadCsvClick(ClickEvent e) {
    uploadCsv.onClick(permissionsTab, e);
  }

  @UiHandler("addNow")
  void onAddUsersClick(ClickEvent e) {
    if (addedUsers.getText().contains("@")) {
      Window.alert("Usernames with '@' are not supported (email accounts are not supported)");
      return;
    }

    List<String> existingUsernames = dataProvider.getList().stream().map(UserSecurityInfo::getUsername).collect(Collectors.toList());
    List<UserSecurityInfo> newAccounts = new ArrayList<>();
    for (String username : addedUsers.getText().split(" \n")) {
      String trimmedUsername = username.trim();
      if (!trimmedUsername.isEmpty() && !existingUsernames.contains(trimmedUsername))
        newAccounts.add(new UserSecurityInfo(trimmedUsername, null, null, UserType.REGISTERED));
    }

    if (!newAccounts.isEmpty()) {
      dataProvider.getList().addAll(newAccounts);
      userTable.setPageSize(Math.max(15, dataProvider.getList().size()));
      uiOutOfSyncWithServer();
    }
    addedUsers.setText("");
  }

  @UiHandler("button")
  void onUpdateClick(ClickEvent e) {
    updateUsersOnServer();
  }

  interface TemporaryAccessConfigurationSheetUiBinder extends
      UiBinder<Widget, AccessConfigurationSheet> {
  }

  private static final class AuthChangeValidation implements
      BooleanValidationPredicate<UserSecurityInfo> {

    final GrantedAuthorityName auth;

    AuthChangeValidation(GrantedAuthorityName auth) {
      this.auth = auth;
    }

    @Override
    public boolean isValid(boolean prospectiveValue, UserSecurityInfo key) {
      // data collector must be an ODK account
      boolean badCollector = auth.equals(GrantedAuthorityName.GROUP_DATA_COLLECTORS)
          && (key.getUsername() == null);
      // site admin must not be the anonymous user
      boolean badSiteAdmin = auth.equals(GrantedAuthorityName.GROUP_SITE_ADMINS)
          && (key.getType() == UserType.ANONYMOUS);
      return !(badCollector || badSiteAdmin);
    }
  }

  private static final class AuthVisiblePredicate implements UIVisiblePredicate<UserSecurityInfo> {

    final GrantedAuthorityName auth;

    AuthVisiblePredicate(GrantedAuthorityName auth) {
      this.auth = auth;
    }

    @Override
    public boolean isVisible(UserSecurityInfo key) {
      if (auth == GrantedAuthorityName.GROUP_SITE_ADMINS) {
        // anonymous user should not be able to be a site admin
        return (key.getType() != UserType.ANONYMOUS);
      }

      if (auth == GrantedAuthorityName.GROUP_DATA_COLLECTORS) {
        // data collectors can only be ODK accounts...
        return (key.getUsername() != null);
      }
      return true;
    }

  }

  private static final class AuthEnabledPredicate implements UIEnabledPredicate<UserSecurityInfo> {

    final GrantedAuthorityName auth;

    AuthEnabledPredicate(GrantedAuthorityName auth) {
      this.auth = auth;
    }

    @Override
    public boolean isEnabled(UserSecurityInfo info) {
      TreeSet<GrantedAuthorityName> assignedGroups = info.getAssignedUserGroups();

      switch (auth) {
        case GROUP_DATA_COLLECTORS:
          // data collectors must be anonymous
          // or an ODK account type
          return (info.getType() == UserType.ANONYMOUS) ||
              (info.getUsername() != null);
        case GROUP_DATA_VIEWERS:
          if (assignedGroups.contains(GrantedAuthorityName.GROUP_FORM_MANAGERS)
              || assignedGroups.contains(GrantedAuthorityName.GROUP_SITE_ADMINS)) {
            return false;
          }
          return true;
        case GROUP_FORM_MANAGERS:
          if (assignedGroups.contains(GrantedAuthorityName.GROUP_SITE_ADMINS)) {
            return false;
          }
          return true;
        case GROUP_SITE_ADMINS:
          // don't let the designated super-user un-check their
          // site admin privileges.
          String email = info.getEmail();
          String superUserEmail = AggregateUI.getUI().getRealmInfo().getSuperUserEmail();
          String username = info.getUsername();
          String superUsername = AggregateUI.getUI().getRealmInfo().getSuperUsername();
          if ((email != null && superUserEmail != null && superUserEmail.equals(email)) ||
              (username != null && superUsername != null && superUsername.equals(username))) {
            return false;
          }
          return true;
        default:
          return false;
      }
    }
  }

  private static final class AuthComparator implements Comparator<UserSecurityInfo> {

    final GrantedAuthorityName auth;

    AuthComparator(GrantedAuthorityName auth) {
      this.auth = auth;
    }

    @Override
    public int compare(UserSecurityInfo arg0, UserSecurityInfo arg1) {
      boolean arg0Contains = arg0.getAssignedUserGroups().contains(auth);
      boolean arg1Contains = arg1.getAssignedUserGroups().contains(auth);

      if (arg0Contains == arg1Contains) {
        // same value. In the case where another
        // assigned granted authority subsumes this one,
        // we want to place the users with subsumed
        // rights above those with no rights.
        arg0Contains = arg0.getGrantedAuthorities().contains(auth);
        arg1Contains = arg1.getGrantedAuthorities().contains(auth);
        if (arg0Contains == arg1Contains)
          return 0;
        if (arg0Contains)
          return -1;
        return 1;
      }
      // checked before unchecked...
      if (arg0Contains)
        return -1;
      return 1;
    }
  }

  private static final class EnableNotAnonymousOrSuperUserPredicate implements
      UIEnabledPredicate<UserSecurityInfo> {
    @Override
    public boolean isEnabled(UserSecurityInfo info) {
      // enable only if it is a registered user
      if (info.getType() != UserType.REGISTERED)
        return false;
      // enable only if the user is not the superUser.
      String email = info.getEmail();
      String superUserEmail = AggregateUI.getUI().getRealmInfo().getSuperUserEmail();
      String username = info.getUsername();
      String superUsername = AggregateUI.getUI().getRealmInfo().getSuperUsername();
      if ((email != null && superUserEmail != null && superUserEmail.equals(email)) ||
          (username != null && superUsername != null && superUsername.equals(username))) {
        return false;
      }
      return true;
    }
  }

  private static final class EnableNotAnonymousPredicate implements
      UIEnabledPredicate<UserSecurityInfo> {
    @Override
    public boolean isEnabled(UserSecurityInfo info) {
      // enable only if it is a registered user
      return (info.getType() == UserType.REGISTERED);
    }
  }

  private static final class EnableLocalAccountPredicate implements
      UIEnabledPredicate<UserSecurityInfo> {
    @Override
    public boolean isEnabled(UserSecurityInfo info) {
      return (info.getType() == UserType.REGISTERED && info.getUsername() != null);
    }
  }

  private static class UsernameComparator implements Comparator<UserSecurityInfo> {
    @Override
    public int compare(UserSecurityInfo arg0, UserSecurityInfo arg1) {
      if (arg0 == arg1)
        return 0;

      if (arg0 != null) {
        return (arg1 != null) ? arg0.getCanonicalName().compareToIgnoreCase(arg1.getCanonicalName()) : 1;
      }
      return -1;
    }
  }

  private static class FullNameComparator implements Comparator<UserSecurityInfo> {
    @Override
    public int compare(UserSecurityInfo arg0, UserSecurityInfo arg1) {
      if (arg0 == arg1)
        return 0;

      if (arg0 != null) {
        return (arg1 != null) ? arg0.getFullName().compareToIgnoreCase(arg1.getFullName()) : 1;
      }
      return -1;
    }
  }

  private class GroupMembershipColumn extends UIEnabledValidatingCheckboxColumn<UserSecurityInfo> {
    final GrantedAuthorityName auth;

    GroupMembershipColumn(GrantedAuthorityName auth) {
      super(new AuthChangeValidation(auth), new AuthVisiblePredicate(auth),
          new AuthEnabledPredicate(auth), new AuthComparator(auth));
      this.auth = auth;
    }

    @Override
    public void setValue(UserSecurityInfo object, Boolean value) {
      if (value) {
        object.getAssignedUserGroups().add(auth);
      } else {
        object.getAssignedUserGroups().remove(auth);
      }
      if (!auth.equals(GrantedAuthorityName.GROUP_DATA_COLLECTORS)) {
        // we may be disabling or enabling some checkboxes...
        userTable.redraw();
        if (object.getType() == UserType.ANONYMOUS) {
          boolean isGDV = object.getAssignedUserGroups().contains(
              GrantedAuthorityName.GROUP_DATA_VIEWERS);
          if (isGDV) {
            anonymousAttachmentViewers.setValue(true, false);
          } else {
            // restore original value to checkbox...
            anonymousAttachmentViewers.setValue(anonymousAttachmentBoolean, false);
          }
          anonymousAttachmentViewers.setEnabled(!isGDV);
        }
      }
      uiOutOfSyncWithServer();
    }

    @Override
    public Boolean getValue(UserSecurityInfo object) {

      TreeSet<GrantedAuthorityName> assignedGroups = object.getAssignedUserGroups();
      switch (auth) {
        case GROUP_DATA_COLLECTORS:
          return assignedGroups.contains(auth);
        case GROUP_DATA_VIEWERS:
          return assignedGroups.contains(GrantedAuthorityName.GROUP_FORM_MANAGERS)
              || assignedGroups.contains(GrantedAuthorityName.GROUP_SITE_ADMINS)
              || assignedGroups.contains(auth);
        case GROUP_FORM_MANAGERS:
          return assignedGroups.contains(GrantedAuthorityName.GROUP_SITE_ADMINS)
              || assignedGroups.contains(auth);
        case GROUP_SITE_ADMINS:
          return assignedGroups.contains(auth);
        default:
          return false;
      }
    }
  }

  private class UpdateUserDisplay implements AsyncCallback<ArrayList<UserSecurityInfo>> {
    @Override
    public void onFailure(Throwable caught) {
      AggregateUI.getUI().reportError(" Impossible de récupérer les utilisateurs du serveur: ", caught);
    }

    @Override
    public void onSuccess(ArrayList<UserSecurityInfo> result) {
      dataProvider.getList().clear();
      addedUsers.setText("");
      for (UserSecurityInfo i : result) {
        if (i.getType() == UserType.ANONYMOUS) {
          TreeSet<GrantedAuthorityName> assignedSet = i.getAssignedUserGroups();
          boolean hasAV = assignedSet.contains(GrantedAuthorityName.ROLE_ATTACHMENT_VIEWER);
          anonymousAttachmentBoolean = hasAV;
          boolean isGDV = i.getAssignedUserGroups().contains(
              GrantedAuthorityName.GROUP_DATA_VIEWERS);
          anonymousAttachmentViewers.setValue(anonymousAttachmentBoolean || isGDV, false);
          anonymousAttachmentViewers.setEnabled(!isGDV);
          break;
        }
      }
      dataProvider.getList().addAll(result);
      userTable.setPageSize(Math.max(15, result.size()));
      uiInSyncWithServer();
    }
  }

  /**
   * Username cannot be null or zero-length. If it is a Google account type (an
   * e-mail address), then it should look like an e-mail address.
   */
  private final class ValidatingUsernamePredicate implements
      StringValidationPredicate<UserSecurityInfo> {

    @Override
    public boolean isValid(String prospectiveValue, UserSecurityInfo key) {
      if (prospectiveValue != null && prospectiveValue.length() != 0) {
        if (prospectiveValue.trim().length() != prospectiveValue.length()) {
          Window.alert("Espace non valide avant ou après le nom d'utilisateur");
          return false;
        }

        if (prospectiveValue.contains("@")) {
          Window.alert("Les noms d'utilisateur avec '@' ne sont pas supportés (les comptes de messagerie ne sont pas supportés)");
          return false;
        }

        // don't allow an edit to convert this name into an existing
        // one.
        for (UserSecurityInfo i : dataProvider.getList()) {
          if (i == key)
            continue;
          if (i.getCanonicalName().equals(prospectiveValue)) {
            Window.alert("Le nom d'utilisateur est déjà défini");
            return false;
          }
        }
        if (key.getUsername() == null) {
          // we are setting an e-mail address... verify it...
          if (EmailParser.hasInvalidEmailCharacters(prospectiveValue)) {
            Window.alert("Caractères non valides dans l'adresse e-mail.\n"
                + "L’adresse électronique ne peut pas contenir d’espace, de guillemets,\n"
                + "virgules, points-virgules ou autre ponctuation");
            return false;
          } else if (prospectiveValue.indexOf(EmailParser.K_AT) == -1) {
            Window.alert("La partie \"@ domain.org\" manque dans l'adresse de messagerie\n"
                + "L'email doit être de la forme 'nomutilisateur@domaine.org'");
            return false;
          }
        }
      } else {
        Window.alert("Le nom d'utilisateur ne peut pas être vide");
        return false;
      }
      return true;
    }

  }

  private final class UsernameTextColumn extends
      UIEnabledValidatingTextInputColumn<UserSecurityInfo> {

    UsernameTextColumn() {
      super(new ValidatingUsernamePredicate(), new EnableNotAnonymousOrSuperUserPredicate(),
          new UsernameComparator());
    }

    @Override
    public String getValue(UserSecurityInfo object) {
      String email = object.getEmail();
      if (email != null) {
        return email.substring(EmailParser.K_MAILTO.length());
      } else {
        return object.getUsername();
      }
    }

    @Override
    public void setValue(UserSecurityInfo object, String value) {
      uiOutOfSyncWithServer();
      // validation happens in the validation predicate...
      if (object.getUsername() == null) {
        object.setEmail(EmailParser.K_MAILTO + value);
      } else {
        object.setUsername(value);
      }
    }
  }

  private final class FullNameTextColumn extends
      UIEnabledValidatingTextInputColumn<UserSecurityInfo> {

    FullNameTextColumn() {
      super(null, new EnableNotAnonymousPredicate(), new FullNameComparator());
    }

    @Override
    public String getValue(UserSecurityInfo object) {
      return object.getFullName();
    }

    @Override
    public void setValue(UserSecurityInfo object, String value) {
      uiOutOfSyncWithServer();
      // validation happens in the validation predicate...
      object.setFullName(value);
    }
  }

  private final class DeleteActionCallback implements
      UIEnabledActionCell.Delegate<UserSecurityInfo> {

    @Override
    public void execute(UserSecurityInfo object) {
      final ConfirmUserDeletePopup popup = new ConfirmUserDeletePopup(object,
          AccessConfigurationSheet.this);
      popup.setPopupPositionAndShow(popup.getPositionCallBack());
    }
  }

  private final class ChangePasswordActionCallback implements
      UIEnabledActionCell.Delegate<UserSecurityInfo> {

    @Override
    public void execute(UserSecurityInfo object) {
      if (isUiOutOfSyncWithServer()) {
        Window
            .alert("Les modifications non enregistrées existent. "
                + "\nEnregistrez les modifications ou réinitialisez-les en actualisant l’écran.\nEnsuite, vous pouvez changer les mots de passe.");
        return;
      }

      final PopupPanel popup = new ChangePasswordPopup(object);
      popup.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
        @Override
        public void setPosition(int offsetWidth, int offsetHeight) {
          int left = ((Window.getScrollLeft() + Window.getClientWidth() - offsetWidth) / 2);
          int top = ((Window.getScrollTop() + Window.getClientHeight() - offsetHeight) / 2);
          popup.setPopupPosition(left, top);
        }
      });
    }
  }
}
