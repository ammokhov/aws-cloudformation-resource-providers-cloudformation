package software.amazon.cloudformation.stack;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.proxy.*;
import software.amazon.cloudformation.resource.IdentifierUtils;

public class DeleteHandler extends BaseCloudformationHandler {

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                          final ResourceHandlerRequest<ResourceModel> request,
                                                                          final CallbackContext callbackContext,
                                                                          final ProxyClient<CloudFormationClient> proxyClient,
                                                                          final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        if (StringUtils.isNullOrEmpty(request.getDesiredResourceState().getStackName()))
            model.setStackName(IdentifierUtils.generateResourceIdentifier(request.getLogicalResourceIdentifier(), request.getClientRequestToken(), MAX_LENGTH_STACK_NAME));

        return proxy.initiate("cfn::delete-stack", proxyClient, request.getDesiredResourceState(), callbackContext)

                .request(Translator::deleteStackRequest)
                //.retry(Exponential.of().minDelay(Duration.ofSeconds(10L)).build())

                .call((stackRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(stackRequest, proxyInvocation.client()::deleteStack))

                .stabilize((stackRequestCallback, stackResponseCallback, proxyInvocationCallback, modelCallback, callbackContextCallback) ->
                        isStackDeleted(proxyInvocationCallback, modelCallback))
                .success();
    }
}
