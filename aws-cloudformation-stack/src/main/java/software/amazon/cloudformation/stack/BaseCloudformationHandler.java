package software.amazon.cloudformation.stack;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.exceptions.ValidationException;

public abstract class BaseCloudformationHandler extends BaseHandler<CallbackContext> {
    private final static String ERROR_MESSAGE_STACK_DOES_NOT_EXIST = "does not exist";
    protected static final int MAX_LENGTH_STACK_NAME = 128;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                       final ResourceHandlerRequest<ResourceModel> request,
                                                                       final CallbackContext cbCxt,
                                                                       final Logger logger) {

        return handleRequest(proxy, request, cbCxt != null ? cbCxt : new CallbackContext(), proxy.newProxy(this::getClient),
                logger);
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(AmazonWebServicesClientProxy proxy,
                                                                                   ResourceHandlerRequest<ResourceModel> request,
                                                                                   CallbackContext callbackContext,
                                                                                   ProxyClient<CloudFormationClient> client,
                                                                                   Logger logger);

    private static class LazyHolder {
        static final CloudFormationClient CFN_CLIENT = CloudFormationClient.create();
    }

    public CloudFormationClient getClient() {
        return LazyHolder.CFN_CLIENT;
    }

    protected boolean isStackCreated(final ProxyClient<CloudFormationClient> proxy,
                                     final ResourceModel model,
                                     final CreateStackResponse createStackResponse) {
        model.setId(createStackResponse.stackId());
        final Stack stack = describeStack(proxy, model);

        if (stack.stackStatus() == StackStatus.CREATE_FAILED)
            throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, stack.stackStatusReason());

        return stack.stackStatus() == StackStatus.CREATE_COMPLETE;
    }

    protected boolean isStackDeleted(final ProxyClient<CloudFormationClient> proxy,
                                     final ResourceModel model) {
        final Stack stack = describeStack(proxy, model);

        try {
            System.out.println(stack.stackStatus());
            if (stack.stackStatus() == StackStatus.DELETE_FAILED)
                throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, stack.stackStatusReason());
            return stack.stackStatus() == StackStatus.DELETE_COMPLETE;

        } catch (ValidationException e) {
            if(e.getMessage().contains(ERROR_MESSAGE_STACK_DOES_NOT_EXIST)) {
                return true;
            }
            throw e;
        }
    }

    protected Stack describeStack(final ProxyClient<CloudFormationClient> proxy,
                                  final ResourceModel model) {
        return proxy.injectCredentialsAndInvokeV2(Translator.describeStacksRequest(model), proxy.client()::describeStacks)
                .stacks().stream().findFirst().get();
    }

}
