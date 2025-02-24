/*
 * Copyright (C) 2009 Google Inc.
 * Copyright (C) 2010 University of Washington.
 * Copyright (C) 2018 Nafundi
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

package org.opendatakit.aggregate.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.codec.binary.Base64;
import org.opendatakit.aggregate.constants.ParserConsts;
import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.datamodel.FormElementModel;
import org.opendatakit.aggregate.datamodel.TopLevelInstanceData;
import org.opendatakit.aggregate.exception.ODKConversionException;
import org.opendatakit.aggregate.exception.ODKFormNotFoundException;
import org.opendatakit.aggregate.exception.ODKFormSubmissionsDisabledException;
import org.opendatakit.aggregate.exception.ODKIncompleteSubmissionData;
import org.opendatakit.aggregate.exception.ODKIncompleteSubmissionData.Reason;
import org.opendatakit.aggregate.exception.ODKParseException;
import org.opendatakit.aggregate.form.FormFactory;
import org.opendatakit.aggregate.form.IForm;
import org.opendatakit.aggregate.submission.Submission;
import org.opendatakit.aggregate.submission.SubmissionField;
import org.opendatakit.aggregate.submission.SubmissionSet;
import org.opendatakit.aggregate.submission.type.BlobSubmissionType;
import org.opendatakit.aggregate.submission.type.RepeatSubmissionType;
import org.opendatakit.aggregate.submission.type.jr.JRTemporal;
import org.opendatakit.common.datamodel.DeleteHelper;
import org.opendatakit.common.datamodel.ODKEnumeratedElementException;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.EntityKey;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.common.persistence.exception.ODKTaskLockException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.utils.WebUtils;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;
import org.opendatakit.common.web.constants.HtmlConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SubmissionParser {

  private static final String OPEN_ROSA_NAMESPACE_PRELIM = "http://openrosa.org/xforms/metadata";
  private static final String OPEN_ROSA_NAMESPACE = "http://openrosa.org/xforms";
  private static final String OPEN_ROSA_NAMESPACE_SLASH = "http://openrosa.org/xforms/";
  private static final String OPEN_ROSA_METADATA_TAG = "meta";
  private static final String OPEN_ROSA_INSTANCE_ID = "instanceID";
  private String formId;
  private IForm form;
  private Element root;
  private Submission submission;
  private boolean preExistingComplete = false;
  private MultiPartFormData submissionFormItems;
  private EntityKey topLevelTableKey = null;

  public SubmissionParser(InputStream inputStreamXML, CallingContext cc) throws IOException, ODKFormNotFoundException, ODKParseException, ODKIncompleteSubmissionData, ODKConversionException, ODKDatastoreException, ODKFormSubmissionsDisabledException, ODKTaskLockException {
    constructorHelper(inputStreamXML, false, cc);
  }

  public SubmissionParser(MultiPartFormData submissionFormParser, boolean isIncomplete, CallingContext cc) throws IOException, ODKFormNotFoundException, ODKParseException, ODKIncompleteSubmissionData, ODKConversionException, ODKDatastoreException, ODKFormSubmissionsDisabledException, ODKTaskLockException {
    if (submissionFormParser == null) {
      // TODO: review best error handling strategy
      throw new IOException("DID NOT GET A MULTIPARTFORMPARSER");
    }
    submissionFormItems = submissionFormParser;
    MultiPartFormItem submission = submissionFormItems
        .getFormDataByFieldName(ServletConsts.XML_SUBMISSION_FILE);
    if (submission == null) {
      // TODO: review best error handling strategy
      throw new IOException("DID NOT GET A SUBMISSION");
    }

    InputStream inputStreamXML = new ByteArrayInputStream(submission.getStream().toByteArray());
    try {
      constructorHelper(inputStreamXML, isIncomplete, cc);
    } finally {
      inputStreamXML.close();
    }
  }

  public Submission getSubmission() {
    return submission;
  }

  public boolean wasPreexistingComplete() {
    return preExistingComplete;
  }

  public IForm getForm() {
    return form;
  }

  private String getOpenRosaInstanceId() {
    Node n = findMetaTag(root);
    if (n != null) {
      NodeList nl = n.getChildNodes();
      for (int i = 0; i < nl.getLength(); ++i) {
        Node cn = nl.item(i);
        String cnUri = cn.getNamespaceURI();
        String cnName = cn.getLocalName();
        if (cn.getNodeType() == Node.ELEMENT_NODE
            && cnName.equals(OPEN_ROSA_INSTANCE_ID)
            && (cnUri == null ||
            cnUri.equalsIgnoreCase(OPEN_ROSA_NAMESPACE) ||
            cnUri.equalsIgnoreCase(OPEN_ROSA_NAMESPACE_SLASH) ||
            cnUri.equalsIgnoreCase(OPEN_ROSA_NAMESPACE_PRELIM))) {
          NodeList cnl = cn.getChildNodes();
          boolean textFound = false;
          int idxText = -1;
          for (int j = 0; j < cnl.getLength(); ++j) {
            Node cnln = cnl.item(j);
            if (cnln.getNodeType() == Node.TEXT_NODE) {
              if (textFound) {
                throw new IllegalStateException("Expected a single text node");
              }
              textFound = true;
              idxText = j;
            }
          }
          if (textFound) {
            return cnl.item(idxText).getNodeValue();
          }
        }
      }
    }
    return null;
  }

  private Node findMetaTag(Node parent) {
    if (parent.getNodeType() != Node.ELEMENT_NODE)
      return null;
    String parentName = parent.getLocalName();
    NodeList nl = parent.getChildNodes();
    for (int i = 0; i < nl.getLength(); ++i) {
      Node n = nl.item(i);
      String namespace = n.getNamespaceURI();
      String name = n.getLocalName();
      if (n.getNodeType() == Node.ELEMENT_NODE
          && name.equals(OPEN_ROSA_METADATA_TAG)
          && (namespace == null ||
          namespace.equalsIgnoreCase(OPEN_ROSA_NAMESPACE) ||
          namespace.equalsIgnoreCase(OPEN_ROSA_NAMESPACE_SLASH) ||
          namespace.equalsIgnoreCase(OPEN_ROSA_NAMESPACE_PRELIM))) {
        return n;
      } else {
        n = findMetaTag(n);
        if (n != null)
          return n;
      }
    }
    return null;
  }

  private void constructorHelper(InputStream inputStreamXML, boolean isIncomplete, CallingContext cc) throws IOException, ODKFormNotFoundException, ODKParseException, ODKIncompleteSubmissionData, ODKConversionException, ODKDatastoreException, ODKFormSubmissionsDisabledException, ODKTaskLockException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setIgnoringComments(true);
      factory.setCoalescing(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(inputStreamXML);
      root = doc.getDocumentElement();
      // if debugging: printNode(root);

      // check for odk id
      formId = root.getAttribute(ParserConsts.FORM_ID_ATTRIBUTE_NAME);

      // if odk id is not present use namespace
      if (formId.equalsIgnoreCase(BasicConsts.EMPTY_STRING)) {
        String schema = root.getAttribute(ParserConsts.NAMESPACE_ATTRIBUTE);

        // TODO: move this into FormDefinition?
        if (schema == null) {
          throw new ODKIncompleteSubmissionData(Reason.ID_MISSING);
        }

        formId = schema;
      }

    } catch (ParserConfigurationException e) {
      throw new IOException(e);
    } catch (SAXException e) {
      e.printStackTrace();
      throw new IOException(e);
    }

    // need to escape all slashes... for xpath processing...
    formId = formId.replaceAll(ParserConsts.FORWARD_SLASH, ParserConsts.FORWARD_SLASH_SUBSTITUTION);

    String fullyQualifiedId = FormFactory.extractWellFormedFormId(formId);

    form = FormFactory.retrieveFormByFormId(fullyQualifiedId, cc);
    if (!form.getSubmissionEnabled()) {
      throw new ODKFormSubmissionsDisabledException();
    }

    String modelVersionString = root.getAttribute(ParserConsts.MODEL_VERSION_ATTRIBUTE_NAME);
    String uiVersionString = root.getAttribute(ParserConsts.UI_VERSION_ATTRIBUTE_NAME);
    Long modelVersion = null;
    Long uiVersion = null;
    if (modelVersionString != null && modelVersionString.length() > 0) {
      modelVersion = Long.valueOf(modelVersionString);
    }
    if (uiVersionString != null && uiVersionString.length() > 0) {
      uiVersion = Long.valueOf(uiVersionString);
    }

    String instanceId = getOpenRosaInstanceId();
    if (instanceId == null) {
      instanceId = root.getAttribute(ParserConsts.INSTANCE_ID_ATTRIBUTE_NAME);
      if (instanceId == null || instanceId.length() == 0) {
        instanceId = CommonFieldsBase.newUri();
      }
    }

    Date submissionDate = new Date();
    String submissionDateString = root.getAttribute(ParserConsts.SUBMISSION_DATE_ATTRIBUTE_NAME);
    if (submissionDateString != null && submissionDateString.length() != 0) {
      submissionDate = JRTemporal.dateTime(submissionDateString).getParsed();
    }

    Date markedAsCompleteDate = new Date();
    String markedAsCompleteDateString = root
        .getAttribute(ParserConsts.MARKED_AS_COMPLETE_DATE_ATTRIBUTE_NAME);
    if (markedAsCompleteDateString != null && markedAsCompleteDateString.length() != 0) {
      markedAsCompleteDate = JRTemporal.dateTime(markedAsCompleteDateString).getParsed();
    }

    SubmissionLockTemplate modificationLock = new SubmissionLockTemplate(formId, instanceId, cc);
    try {
      modificationLock.acquire();
      // retrieve the record with this instanceId from the database or
      // create a new one. This supports submissions having more than
      // 10MB of attachments. In that case, ODK Collect will post the
      // submission in multiple parts and Aggregate needs to be able to
      // merge the parts together. This SHOULD NOT be used to 'update'
      // an existing submission, only to attach additional binary content
      // to an already-uploaded submission.
      boolean preExisting = false;
      try {
        Datastore ds = cc.getDatastore();
        User user = cc.getCurrentUser();
        TopLevelInstanceData fi = (TopLevelInstanceData) ds.getEntity(form.getTopLevelGroupElement()
            .getFormDataModel().getBackingObjectPrototype(), instanceId, user);
        try {
          submission = new Submission(fi, form, cc);
        } catch (ODKDatastoreException e) {
          Logger logger = LoggerFactory.getLogger(Submission.class);
          e.printStackTrace();
          logger.error("Impossible de reconstruire la soumission pour " + fi.getSchemaName() + "."
              + fi.getTableName() + " uri " + fi.getUri());
          if ((e instanceof ODKEntityNotFoundException) ||
              (e instanceof ODKEnumeratedElementException)) {
            // this is a malformed submission...
            // try to clean this up...
            DeleteHelper.deleteDamagedSubmission(fi, form.getAllBackingObjects(), cc);
          }
          throw e;
        }
        preExisting = true;
        preExistingComplete = submission.isComplete();
      } catch (ODKEntityNotFoundException e) {
        submission = new Submission(modelVersion, uiVersion, instanceId, form, submissionDate, cc);
      }

      topLevelTableKey = submission.getKey();

      Map<String, Integer> repeatGroupIndices = new HashMap<String, Integer>();
      FormElementModel formRoot = form.getTopLevelGroupElement();
      // if the submission is pre-existing in the datastore, ONLY update binaries
      boolean uploadAllBinaries = processSubmissionElement(formRoot, root, submission,
          repeatGroupIndices, preExisting, cc);
      submission.setIsComplete(uploadAllBinaries);
      if (uploadAllBinaries) {
        submission.setMarkedAsCompleteDate(markedAsCompleteDate);
      }
      // save the elements inserted into the top-level submission
      try {
        submission.persist(cc);
      } catch (Exception e) {
        List<EntityKey> keys = new ArrayList<EntityKey>();
        submission.recursivelyAddEntityKeysForDeletion(keys, cc);
        keys.add(submission.getKey());
        try {
          DeleteHelper.deleteEntities(keys, cc);
        } catch (Exception ex) {
          // ignore... we are rolling back...
        }
        throw new ODKDatastoreException("Impossible de conserver les données", e);
      }
    } finally {
      modificationLock.release();
    }
  }

  private boolean processSubmissionElement(FormElementModel node, Element currentSubmissionElement, SubmissionSet submissionSet, Map<String, Integer> repeatGroupIndicies, boolean preExisting, CallingContext cc) throws ODKParseException, ODKConversionException, ODKDatastoreException {

    if (node == null || currentSubmissionElement == null) {
      return true;
    }

    // the element name of the fdm is the tag name...
    String submissionTag = node.getElementName();
    if (submissionTag == null) {
      return true;
    }

    // verify that the xml matches the node we are processing...
    if (!currentSubmissionElement.getLocalName().equals(submissionTag)) {
      throw new ODKParseException("Balise d'élément de document XML: "
          + currentSubmissionElement.getLocalName()
          + " ne correspond pas au nom de balise du modèle de données xform: " + submissionTag);
    }

    // get the structure under the fdm tag name...
    List<Element> elements = getElements(currentSubmissionElement);
    if (elements.size() == 0) {
      return true; // the group is not relevant...
    }
    // and for each of these, they should be fields under the given fdm
    // and values within the submissionSet
    boolean complete = true;
    for (Element e : elements) {
      FormElementModel m = node.findElementByName(e.getLocalName());
      if (m == null) {
        continue;
        // throw new ODKParseException();
      }
      switch (m.getElementType()) {
        case METADATA:
          // This keeps lint warnings down
          break;
        case GROUP:
          // need to recurse on these elements keeping the same
          // submissionSet...
          complete = complete
              & processSubmissionElement(m, e, submissionSet, repeatGroupIndicies, preExisting, cc);
          break;
        case REPEAT:
          // get the field that will hold the repeats...
          // get the repeat group...
          RepeatSubmissionType repeats = (RepeatSubmissionType) submissionSet.getElementValue(m);

          // determine the ordinal of the repeat group element we are processing.
          // do this by constructing the submission key for the repeat group and
          // seeing if that key is in the repeatGroupIndicies table. If not, the
          // ordinal is 1L. Otherwise, it is the value in the table plus 1L.
          String fullName = repeats.constructSubmissionKey().toString();
          Integer idx = repeatGroupIndicies.get(fullName);
          if (idx == null) {
            idx = 1; // base case -- not yet in repeatGroupIndicies map
          } else {
            ++idx;
          }
          // save the updated index
          repeatGroupIndicies.put(fullName, idx);

          // get or create the instance's submission set for this ordinal
          SubmissionSet repeatableSubmissionSet;
          if (repeats.getNumberRepeats() >= idx) {
            // we already have this set defined
            repeatableSubmissionSet = repeats.getSubmissionSets().get(idx - 1);
          } else if (repeats.getNumberRepeats() == idx - 1) {
            // Create a submission set for a new instance...
            long l = repeats.getNumberRepeats() + 1L;
            repeatableSubmissionSet = new SubmissionSet(submissionSet, l, m, form, topLevelTableKey,
                cc);
            repeats.addSubmissionSet(repeatableSubmissionSet);
          } else {
            throw new IllegalStateException("incrémentation de répétitions de plus d'un!");
          }
          // populate the instance's submission set with values from e...
          complete = complete
              & processSubmissionElement(m, e, repeatableSubmissionSet, repeatGroupIndicies,
              preExisting, cc);
          break;
        case STRING:
        case INTEGER:
        case DECIMAL:
        case BOOLEAN:
        case GEOTRACE:
        case GEOSHAPE:
        case SELECT1: // identifies SelectChoice table
        case SELECTN: // identifies SelectChoice table
          if (!preExisting) {
            String value = getSubmissionValue(e);
            SubmissionField<?> subField = (SubmissionField<?>) submissionSet.getElementValue(m);
            subField.setValueFromString(value);
          }
          break;
        case JRDATE:
          if (!preExisting) {
            String value = getSubmissionValue(e);
            ((SubmissionField<?>) submissionSet.getElementValue(m)).setValueFromString(value);
          }
          break;
        case JRTIME:
          if (!preExisting) {
            String value = getSubmissionValue(e);
            ((SubmissionField<?>) submissionSet.getElementValue(m)).setValueFromString(value);
          }
          break;
        case JRDATETIME:
          if (!preExisting) {
            String value = getSubmissionValue(e);
            ((SubmissionField<?>) submissionSet.getElementValue(m)).setValueFromString(value);
          }
          break;
        case GEOPOINT:
          if (!preExisting) {
            String value = getSubmissionValue(e);
            ((SubmissionField<?>) submissionSet.getElementValue(m)).setValueFromString(value);
          }
          break;
        case BINARY: // identifies BinaryContent table
        {
          String value = getSubmissionValue(e);
          SubmissionField<?> submissionElement = ((SubmissionField<?>) submissionSet
              .getElementValue(m));
          complete = complete & processBinarySubmission(m, submissionElement, value, cc);
        }
        break;
      }
    }
    return complete;
  }

  private boolean processBinarySubmission(FormElementModel m, SubmissionField<?> submissionElement, String value, CallingContext cc) throws ODKDatastoreException {

    // value will either be a byte array (if not a multipart/form-data
    // submission)
    // or the filename of the attached media file
    // or the name of the filed holding the media file contents.
    // It will be null only if there is no associated file.
    if (value == null)
      return true;

    // check to see if we received a multipart submission
    if (submissionFormItems == null) {
      // TODO: problem, only accept a base64 encoded in a direct XML post
      byte[] receivedBytes = Base64.decodeBase64(value.getBytes());
      // TODO: problem since we don't know how to tell what type of
      // binary without content type, defaulting to JPG
      submissionElement.setValueFromByteArray(receivedBytes, HtmlConsts.RESP_TYPE_IMAGE_JPEG,
          null, false, cc);
    } else {
      // attempt to find binary data in multi-part form submission
      // first searching by file name, then field name
      MultiPartFormItem binaryData = submissionFormItems.getFormDataByFileName(value);
      if (binaryData == null) {
        binaryData = submissionFormItems.getFormDataByFieldName(value);
      }
      // after completing the search now check if found anything and
      // value, otherwise output error
      if (binaryData != null) {
        // determine the filename, if any...
        String fileName = binaryData.getFilename();
        if (fileName == null || fileName.length() == 0) {
          fileName = null;
        }
        byte[] byteArray = binaryData.getStream().toByteArray();
        submissionElement.setValueFromByteArray(byteArray, binaryData.getContentType(),
            fileName, false, cc);
      } else {
        // Assume the value is the filename...
        submissionElement.setValueFromByteArray(null, null, value, false, cc);

        // and if we already have the content loaded, the content hash will be
        // non-null
        BlobSubmissionType blob = (BlobSubmissionType) submissionElement;
        return (blob.getContentHash(1, cc) != null);
      }
    }
    return true;
  }

  private List<Element> getElements(Element rootNode) {
    List<Element> elements = new ArrayList<Element>();

    NodeList nodeList = rootNode.getChildNodes();

    for (int i = 0; i < nodeList.getLength(); ++i) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        elements.add((Element) node);
      }
    }
    return elements;
  }

  private String getSubmissionValue(Element element) {
    // could not find element, return null
    if (element == null) {
      return null;
    }

    NodeList childNodeList = element.getChildNodes();
    // get element value
    for (int i = 0; i < childNodeList.getLength(); i++) {
      Node node = childNodeList.item(i);
      if (node.getNodeType() == Node.TEXT_NODE) {
        String value = node.getNodeValue().trim();
        if (value.length() > 0) {
          return value;
        } // else go to next node
      }
    } // else has no value so continue

    return null;
  }

}
