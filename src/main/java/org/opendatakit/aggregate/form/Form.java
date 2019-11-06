/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.aggregate.form;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.opendatakit.aggregate.client.form.FormSummary;
import org.opendatakit.aggregate.constants.HtmlUtil;
import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.datamodel.FormElementModel;
import org.opendatakit.aggregate.datamodel.FormElementModel.ElementType;
import org.opendatakit.aggregate.parser.MultiPartFormItem;
import org.opendatakit.aggregate.servlet.FormXmlServlet;
import org.opendatakit.aggregate.submission.SubmissionKeyPart;
import org.opendatakit.common.datamodel.BinaryContentManipulator;
import org.opendatakit.common.datamodel.BinaryContentManipulator.BlobSubmissionOutcome;
import org.opendatakit.common.datamodel.DynamicCommonFieldsBase;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.EntityKey;
import org.opendatakit.common.persistence.Query;
import org.opendatakit.common.persistence.Query.FilterOperation;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;

/**
 * Implementation of the IForm interface.
 * Form objects can be shared across multiple threads.
 *
 * @author mitchellsundt@gmail.com
 */
class Form implements IForm {

  /*
   * Following public fields are valid after the first successful call to
   * getFormDefinition()
   */

  private final FormInfoTable infoRow;

  private final FormInfoFilesetTable filesetRow;

  private final BinaryContentManipulator xform;

  private final BinaryContentManipulator manifest;

  private final FormDefinition formDefinition;

  private final Map<String, FormElementModel> repeatElementMap;

  Form(FormInfoTable infoRow, CallingContext cc) throws ODKDatastoreException {
    Datastore ds = cc.getDatastore();
    User user = cc.getCurrentUser();

    this.infoRow = infoRow;
    String topLevelAuri = infoRow.getUri();

    Query q;
    List<? extends CommonFieldsBase> rows;

    {
      // get fileset (for now, zero or one record)
      FormInfoFilesetTable filesetRelation = FormInfoFilesetTable.assertRelation(cc);
      q = ds.createQuery(filesetRelation, "Form.constructor", user);
      q.addFilter(filesetRelation.topLevelAuri, FilterOperation.EQUAL, topLevelAuri);

      rows = q.executeQuery();
      if (rows.size() == 0) {
        filesetRow = ds.createEntityUsingRelation(filesetRelation, user);
        filesetRow.setTopLevelAuri(topLevelAuri);
        filesetRow.setParentAuri(topLevelAuri);
        filesetRow.setOrdinalNumber(1L);
      } else if (rows.size() == 1) {
        filesetRow = (FormInfoFilesetTable) rows.get(0);
      } else {
        throw new IllegalStateException("more than one fileset!");
      }
    }

    this.xform = FormInfoFilesetTable.assertXformManipulator(topLevelAuri, filesetRow.getUri(), cc);

    this.manifest = FormInfoFilesetTable.assertManifestManipulator(topLevelAuri,
        filesetRow.getUri(), cc);

    formDefinition = FormDefinition.getFormDefinition(infoRow.getStringField(FormInfoTable.FORM_ID), cc);

    repeatElementMap = new HashMap<String, FormElementModel>();
    if (formDefinition != null) {
      populateRepeatElementMap(formDefinition.getTopLevelGroupElement());
    }
  }

  Form(XFormParameters rootElementDefn, boolean isEncryptedForm, boolean isDownloadEnabled,
       String title, CallingContext cc) throws ODKDatastoreException {
    Datastore ds = cc.getDatastore();
    User user = cc.getCurrentUser();

    FormInfoTable infoRelation = FormInfoTable.assertRelation(cc);

    String formUri = CommonFieldsBase.newMD5HashUri(rootElementDefn.formId);

    Date now = new Date();
    infoRow = ds.createEntityUsingRelation(infoRelation, user);
    infoRow.setStringField(infoRow.primaryKey, formUri);
    infoRow.setSubmissionDate(now);
    infoRow.setMarkedAsCompleteDate(now);
    infoRow.setIsComplete(true);
    infoRow.setModelVersion(1L); // rollback (v1.0.x) compatibility
    infoRow.setUiVersion(0L);    // rollback (v1.0.x) compatibility
    infoRow.setStringField(FormInfoTable.FORM_ID, rootElementDefn.formId);

    String topLevelAuri = infoRow.getUri();

    {
      // get fileset (for now, zero or one record)
      FormInfoFilesetTable filesetRelation = FormInfoFilesetTable.assertRelation(cc);
      filesetRow = ds.createEntityUsingRelation(filesetRelation, user);
      filesetRow.setTopLevelAuri(topLevelAuri);
      filesetRow.setParentAuri(topLevelAuri);
      filesetRow.setOrdinalNumber(1L);
      filesetRow.setLongField(FormInfoFilesetTable.ROOT_ELEMENT_MODEL_VERSION,
          rootElementDefn.modelVersion);
      filesetRow.setBooleanField(FormInfoFilesetTable.IS_ENCRYPTED_FORM, isEncryptedForm);
      filesetRow.setBooleanField(FormInfoFilesetTable.IS_DOWNLOAD_ALLOWED, isDownloadEnabled);
      filesetRow.setStringField(FormInfoFilesetTable.FORM_NAME, title);
    }

    this.xform = FormInfoFilesetTable.assertXformManipulator(topLevelAuri, filesetRow.getUri(), cc);

    this.manifest = FormInfoFilesetTable.assertManifestManipulator(topLevelAuri,
        filesetRow.getUri(), cc);

    formDefinition = FormDefinition.getFormDefinition(rootElementDefn.formId, cc);

    repeatElementMap = new HashMap<String, FormElementModel>();
    if (formDefinition != null) {
      populateRepeatElementMap(formDefinition.getTopLevelGroupElement());
    }
  }

  public synchronized void persist(CallingContext cc) throws ODKDatastoreException {
    Datastore ds = cc.getDatastore();
    User user = cc.getCurrentUser();

    ds.putEntity(infoRow, user);
    ds.putEntity(filesetRow, user);
    manifest.persist(cc);
    xform.persist(cc);

    if (formDefinition != null) {
      formDefinition.persistSubmissionAssociation(cc);
    }
  }

  public synchronized void deleteForm(CallingContext cc) throws ODKDatastoreException {
    FormFactory.clearForm(this);
    if (formDefinition != null) {
      // delete the data model normally
      formDefinition.deleteDataModel(cc);
    } else {
      FormDefinition.deleteAbnormalModel(infoRow.getStringField(FormInfoTable.FORM_ID), cc);
    }

    Datastore ds = cc.getDatastore();
    User user = cc.getCurrentUser();
    // delete everything in formInfo

    manifest.deleteAll(cc);
    xform.deleteAll(cc);
    ds.deleteEntity(filesetRow.getEntityKey(), user);
    ds.deleteEntity(infoRow.getEntityKey(), user);
  }

  public EntityKey getEntityKey() {
    return infoRow.getEntityKey();
  }

  public FormElementModel getTopLevelGroupElement() {
    return formDefinition.getTopLevelGroupElement();
  }

  public String getMajorMinorVersionString() {

    Long modelVersion = filesetRow.getLongField(FormInfoFilesetTable.ROOT_ELEMENT_MODEL_VERSION);
    StringBuilder b = new StringBuilder();
    if (modelVersion != null) {
      b.append(modelVersion.toString());
    }
    return b.toString();
  }

  public String getOpenRosaVersionString() {

    Long modelVersion = filesetRow.getLongField(FormInfoFilesetTable.ROOT_ELEMENT_MODEL_VERSION);
    StringBuilder b = new StringBuilder();
    if (modelVersion != null) {
      b.append(modelVersion.toString());
    }
    return b.toString();
  }

  @Override
  public String getXFormFileHash(CallingContext cc) throws ODKDatastoreException {
    return xform.getContentHash(1, cc);
  }

  public boolean hasValidFormDefinition() {
    return (formDefinition != null);
  }

  public String getFormId() {
    return infoRow.getStringField(FormInfoTable.FORM_ID);
  }

  public boolean hasManifestFileset(CallingContext cc) throws ODKDatastoreException {
    return manifest.getAttachmentCount(cc) != 0;
  }

  public BinaryContentManipulator getManifestFileset() {
    return manifest;
  }

  public String getViewableName() {
    return filesetRow.getStringField(FormInfoFilesetTable.FORM_NAME);
  }

  public void setViewableName(String title) {
    if (!filesetRow.setStringField(FormInfoFilesetTable.FORM_NAME, title)) {
      String str = "Overflow on " + FormInfoFilesetTable.FORM_NAME;
      throw new IllegalStateException(str);
    }
  }

  public String getViewableFormNameSuitableAsFileName() {
    String name = getViewableName();
    // any non-alphanumeric is replaced with underscore
    return name.replaceAll("[^\\p{L}0-9]", "_");
  }

  public XFormParameters getRootElementDefn() {
    XFormParameters p = new XFormParameters(infoRow.getStringField(FormInfoTable.FORM_ID),
        filesetRow.getLongField(FormInfoFilesetTable.ROOT_ELEMENT_MODEL_VERSION));
    return p;
  }

  public String getDescription() {
    return filesetRow.getStringField(FormInfoFilesetTable.DESCRIPTION);
  }

  public String getDescriptionUrl() {
    return filesetRow.getStringField(FormInfoFilesetTable.DESCRIPTION_URL);
  }

  public Date getCreationDate() {
    return infoRow.getCreationDate();
  }

  public Date getLastUpdateDate() {
    return infoRow.getLastUpdateDate();
  }

  public String getCreationUser() {
    return infoRow.getCreatorUriUser();
  }

  public BinaryContentManipulator getXformDefinition() {
    return xform;
  }

  public String getFormFilename(CallingContext cc) throws ODKDatastoreException {
    if (xform.getAttachmentCount(cc) == 1) {
      return xform.getUnrootedFilename(1, cc);
    } else if (xform.getAttachmentCount(cc) > 1) {
      throw new IllegalStateException("N'attend qu'un seul enregistrement de groupe de fichiers à la fois!");
    }
    return null;
  }

  public String getFormXml(CallingContext cc) throws ODKDatastoreException {
    if (xform.getAttachmentCount(cc) == 1) {
      if (xform.getContentHash(1, cc) == null) {
        return null;
      }
      byte[] byteArray = xform.getBlob(1, cc);
      try {
        return new String(byteArray, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
        throw new IllegalStateException("Le jeu de caractères UTF-8 n'est pas supporté!");
      }
    } else if (xform.getAttachmentCount(cc) > 1) {
      throw new IllegalStateException("N'attend qu'un seul enregistrement de groupe de fichiers à la fois!");
    }
    return null;
  }

  public Boolean isEncryptedForm() {
    return filesetRow.getBooleanField(FormInfoFilesetTable.IS_ENCRYPTED_FORM);
  }

  public Boolean getDownloadEnabled() {
    return filesetRow.getBooleanField(FormInfoFilesetTable.IS_DOWNLOAD_ALLOWED);
  }

  public void setDownloadEnabled(Boolean downloadEnabled) {
    filesetRow.setBooleanField(FormInfoFilesetTable.IS_DOWNLOAD_ALLOWED, downloadEnabled);
  }

  public Boolean getSubmissionEnabled() {
    // if the form definition doesn't exist, we can't accept submissions
    // this is a transient condition when in the midst of deleting a form or
    // uploading one
    // and another user attempts to list the available forms.
    if (formDefinition == null)
      return false;
    return formDefinition.getIsSubmissionAllowed();
  }

  public void setSubmissionEnabled(Boolean submissionEnabled) {
    formDefinition.setIsSubmissionAllowed(submissionEnabled);
  }

  public FormElementModel getFormElementModel(List<SubmissionKeyPart> submissionKeyParts) {
    FormElementModel m = null;
    boolean formIdElement = true;
    for (SubmissionKeyPart p : submissionKeyParts) {
      if (formIdElement) {
        if (!p.getElementName().equals(getFormId())) {
          return null;
        }
        formIdElement = false;
      } else if (m == null) {
        m = getTopLevelGroupElement();
        if (!p.getElementName().equals(m.getElementName())) {
          return null;
        }
      } else {
        boolean found = false;
        for (FormElementModel c : m.getChildren()) {
          if (c.getElementName().equals(p.getElementName())) {
            m = c;
            found = true;
            break;
          }
        }
        if (!found) {
          return null;
        }
      }
    }
    return m;
  }

  private void getRepeatGroupsInModelHelper(FormElementModel current,
                                            List<FormElementModel> accumulation) {
    for (FormElementModel m : current.getChildren()) {
      if (m.getElementType() == FormElementModel.ElementType.REPEAT) {
        accumulation.add(m);
      }
      getRepeatGroupsInModelHelper(m, accumulation);
    }
  }

  public Set<DynamicCommonFieldsBase> getAllBackingObjects() {
    Set<DynamicCommonFieldsBase> set = new TreeSet<DynamicCommonFieldsBase>(
        DynamicCommonFieldsBase.sameTableName);

    getAllBackingObjectsHelper(getTopLevelGroupElement(), set);
    return set;
  }

  private void getAllBackingObjectsHelper(FormElementModel current, Set<DynamicCommonFieldsBase> set) {
    for (FormElementModel m : current.getChildren()) {
      set.add((DynamicCommonFieldsBase) m.getFormDataModel().getBackingObjectPrototype());
      getAllBackingObjectsHelper(m, set);
    }
  }

  public List<FormElementModel> getRepeatGroupsInModel() {
    List<FormElementModel> list = new ArrayList<FormElementModel>();

    getRepeatGroupsInModelHelper(getTopLevelGroupElement(), list);
    return list;
  }

  private void populateRepeatElementMap(FormElementModel node) {
    if (node == null) {
      return;
    }
    if (node.getElementType() == ElementType.REPEAT) {
      // TODO: this should be fully qualified element name or
      // you could get collisions.
      repeatElementMap.put(node.getElementName(), node);
    }
    List<FormElementModel> children = node.getChildren();
    if (children == null) {
      return;
    }
    for (FormElementModel child : children) {
      populateRepeatElementMap(child);
    }
  }

  public FormSummary generateFormSummary(CallingContext cc) throws ODKDatastoreException {
    if (hasValidFormDefinition()) {
      boolean submit = getSubmissionEnabled();
      boolean downloadable = getDownloadEnabled();
      Map<String, String> xmlProperties = new HashMap<String, String>();
      xmlProperties.put(ServletConsts.FORM_ID, getFormId());
      xmlProperties.put(ServletConsts.HUMAN_READABLE, BasicConsts.TRUE);

      String viewableURL = HtmlUtil.createHrefWithProperties(
          cc.getWebApplicationURL(FormXmlServlet.WWW_ADDR), xmlProperties, getViewableName(), false);
      int mediaFileCount = getManifestFileset().getAttachmentCount(cc);
      return new FormSummary(getViewableName(), getFormId(), getCreationDate(), getCreationUser(),
          downloadable, submit, viewableURL, mediaFileCount);
    } else {
      Map<String, String> xmlProperties = new HashMap<String, String>();
      xmlProperties.put(ServletConsts.FORM_ID, getFormId());
      xmlProperties.put(ServletConsts.HUMAN_READABLE, BasicConsts.TRUE);

      String viewableName = (filesetRow == null) ? getFormId() : getViewableName();
      if (viewableName == null) {
        viewableName = getFormId();
      }
      viewableName = "<<Broken>> " + viewableName;
      String viewableURL = HtmlUtil.createHrefWithProperties(
          cc.getWebApplicationURL(FormXmlServlet.WWW_ADDR), xmlProperties, viewableName, false);

      int mediaFileCount = (getManifestFileset() == null) ? 0 : getManifestFileset().getAttachmentCount(cc);
      return new FormSummary(viewableName, getFormId(), getCreationDate(), getCreationUser(),
          false, false, viewableURL, mediaFileCount);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Form)) {
      return false;
    }
    Form other = (Form) obj;
    if (infoRow == null)
      return (other.infoRow == null);

    return (infoRow.getUri().equals(other.infoRow.getUri()));
  }

  @Override
  public int hashCode() {
    int hashCode = 13;
    if (infoRow != null)
      hashCode += infoRow.getUri().hashCode();
    return hashCode;
  }

  @Override
  public String toString() {
    return getViewableName();
  }

  public String getMd5HashFormXml(CallingContext cc) throws ODKDatastoreException {
    if (xform.getAttachmentCount(cc) == 1) {
      String contentHash = xform.getContentHash(1, cc);
      if (contentHash != null) {
        return contentHash;
      }
      return null;
    } else {
      throw new IllegalStateException("Fichiers XML inexistants ou à formulaires multiples associés à: " + getFormId());
    }
  }

  public BlobSubmissionOutcome setFormXml(String formFilename, String xmlForm, Long modelVersion, CallingContext cc) throws ODKDatastoreException {
    byte[] bytes;
    try {
      bytes = xmlForm.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      throw new IllegalStateException("unexpected", e);
    }
    filesetRow.setLongField(FormInfoFilesetTable.ROOT_ELEMENT_MODEL_VERSION, modelVersion);
    if (xform.getAttachmentCount(cc) == 0) {
      return xform.setValueFromByteArray(bytes, "text/xml", formFilename, false, cc);
    } else {
      String curName = xform.getUnrootedFilename(1, cc);
      String newName = formFilename;
      if ((newName == null) ? (curName == null) : newName.equals(curName)) {
        return xform.setValueFromByteArray(bytes, "text/xml", curName, true, cc);
      } else {
        BlobSubmissionOutcome outcome;
        outcome = xform.setValueFromByteArray(bytes, "text/xml", curName, true, cc);
        if (!xform.renameFilePath(curName, newName, cc)) {
          throw new IllegalStateException("Changement de nom persistant d'échec inattendu");
        }
        return outcome;
      }
    }
  }

  public void setIsComplete(Boolean value) {
    infoRow.setIsComplete(value);
  }

  public EntityKey getKey() {
    return infoRow.getEntityKey();
  }

  public boolean setXFormMediaFile(MultiPartFormItem item, boolean overwriteOK, CallingContext cc) throws ODKDatastoreException {
    String filePath = item.getFilename();
    if (filePath.indexOf("/") != -1) {
      filePath = filePath.substring(filePath.indexOf("/") + 1);
    }
    byte[] byteArray = item.getStream().toByteArray();
    BlobSubmissionOutcome outcome =
        manifest.setValueFromByteArray(byteArray, item.getContentType(), filePath, overwriteOK, cc);
    return (outcome == BlobSubmissionOutcome.NEW_FILE_VERSION);
  }

  public String getUri() {
    return infoRow.getUri();
  }

  @Override
  public boolean isValid() {
    return filesetRow.hasField(FormInfoFilesetTable.IS_DOWNLOAD_ALLOWED);
  }
}
