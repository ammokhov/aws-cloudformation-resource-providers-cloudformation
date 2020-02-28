package software.amazon.cloudformation.stack;

import com.amazonaws.util.StringUtils;
import java.time.Duration;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.proxy.*;
import software.amazon.cloudformation.proxy.delay.Exponential;
import software.amazon.cloudformation.resource.IdentifierUtils;

public class CreateHandler extends BaseCloudformationHandler {
    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                          final ResourceHandlerRequest<ResourceModel> request,
                                                                          final CallbackContext callbackContext,
                                                                          final ProxyClient<CloudFormationClient> proxyClient,
                                                                          final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        if (StringUtils.isNullOrEmpty(request.getDesiredResourceState().getStackName()))
            model.setStackName(IdentifierUtils.generateResourceIdentifier(request.getLogicalResourceIdentifier(), request.getClientRequestToken(), MAX_LENGTH_STACK_NAME));

        return proxy.initiate("cfn::create-stack", proxyClient, request.getDesiredResourceState(), callbackContext)

                .request(Translator::createStackRequest)
                //.retry(Exponential.of().minDelay(Duration.ofSeconds(10L)).build())

                .call((stackRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(stackRequest, proxyInvocation.client()::createStack))

                .stabilize((stackRequestCallback, stackResponseCallback, proxyInvocationCallback, modelCallback, callbackContextCallback) ->
                        isStackCreated(proxyInvocationCallback, modelCallback, stackResponseCallback))
                .success();
    }
}
