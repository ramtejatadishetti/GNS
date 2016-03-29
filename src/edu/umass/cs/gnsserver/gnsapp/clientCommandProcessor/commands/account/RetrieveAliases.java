/*
 *
 *  Copyright (c) 2015 University of Massachusetts
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you
 *  may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 *  Initial developer(s): Westy
 *
 */
package edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commands.account;

import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.AccessSupport;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.AccountAccess;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.AccountInfo;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.CommandResponse;
import static edu.umass.cs.gnscommon.GnsProtocol.*;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commandSupport.GuidInfo;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commands.CommandModule;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.commands.GnsCommand;
import edu.umass.cs.gnsserver.gnsapp.clientCommandProcessor.ClientRequestHandlerInterface;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author westy
 */
public class RetrieveAliases extends GnsCommand {

  /**
   * Creates a RetriveAliases instance.
   *
   * @param module
   */
  public RetrieveAliases(CommandModule module) {
    super(module);
  }

  @Override
  public String[] getCommandParameters() {
    return new String[]{GUID, SIGNATURE, SIGNATUREFULLMESSAGE};
  }

  @Override
  public String getCommandName() {
    return RETRIEVE_ALIASES;
  }

  @Override
  public CommandResponse<String> execute(JSONObject json, ClientRequestHandlerInterface handler) throws InvalidKeyException, InvalidKeySpecException,
          JSONException, NoSuchAlgorithmException, SignatureException, UnsupportedEncodingException {
    String guid = json.getString(GUID);
    String signature = json.getString(SIGNATURE);
    String message = json.getString(SIGNATUREFULLMESSAGE);
    GuidInfo guidInfo;
    if ((guidInfo = AccountAccess.lookupGuidInfo(guid, handler)) == null) {
      return new CommandResponse<String>(BAD_RESPONSE + " " + BAD_GUID + " " + guid);
    }
    if (AccessSupport.verifySignature(guidInfo.getPublicKey(), signature, message)) {
      AccountInfo accountInfo = AccountAccess.lookupAccountInfoFromGuid(guid, handler);
      if (accountInfo == null) {
        return new CommandResponse<String>(BAD_RESPONSE + " " + BAD_ACCOUNT + " " + guid);
      } else if (!accountInfo.isVerified()) {
        return new CommandResponse<String>(BAD_RESPONSE + " " + VERIFICATION_ERROR + " Account not verified");
      }
      ArrayList<String> aliases = accountInfo.getAliases();
      return new CommandResponse<String>(new JSONArray(aliases).toString());
    } else {
      return new CommandResponse<String>(BAD_RESPONSE + " " + BAD_SIGNATURE);
    }
  }

  @Override
  public String getCommandDescription() {
    return "Retrieves all aliases for the account associated with the GUID. Must be signed by the guid. Returns "
            + BAD_GUID + " if the GUID has not been registered.";

  }
}