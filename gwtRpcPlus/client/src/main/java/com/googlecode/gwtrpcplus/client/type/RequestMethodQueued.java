package com.googlecode.gwtrpcplus.client.type;

import java.util.PriorityQueue;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.googlecode.gwtrpcplus.client.impl.AbstractRequestMethod;
import com.googlecode.gwtrpcplus.client.util.MyScheduler;
import com.googlecode.gwtrpcplus.client.util.RequestHelper;
import com.googlecode.gwtrpcplus.shared.InternalServerException;
import com.googlecode.gwtrpcplus.shared.PrioritisedRequest;
import com.googlecode.gwtrpcplus.shared.TimeoutException;


public class RequestMethodQueued extends AbstractRequestMethod {
  // Visible for tests only
  public final static String REQUEST_NAME = "q";

  private final String serviceName;

  private final boolean resendAllowed;

  private final int paralellRequests;

  public RequestMethodQueued(String serviceName, int paralellRequests, boolean resendAllowed) {
    this.serviceName = serviceName;
    this.resendAllowed = resendAllowed;
    this.paralellRequests = paralellRequests;
  }

  protected MyScheduler scheduler;

  private void scheduleFinaly(ScheduledCommand cmd) {
    if (scheduler == null)
      scheduler = new MyScheduler.DefaultScheduler();
    scheduler.scheduleFinaly(cmd);
  }

  private final class QueueRequest implements RequestPlus, Comparable<QueueRequest> {
    private final String requestData;
    private final RequestCallback callback;

    private boolean pending = false;
    private boolean canceled = false;

    private double priority = 0;

    private final PrioritisedRequest request = new PrioritisedRequest() {
      @Override
      public void cancel() {
        if (pending) {
          canceled = true;
        } else
          cancelQueue(QueueRequest.this);
      }

      @Override
      public boolean isPending() {
        return pending;
      }

      @Override
      public void setPriority(double priority) {
        if (!pending && QueueRequest.this.priority != priority) {
          QueueRequest.this.priority = priority;
          // Readd it
          cancelQueue(QueueRequest.this);
          queue(QueueRequest.this);
        }
      }
    };

    @Override
    public String getRequestTypeName() {
      return REQUEST_NAME;
    }

    private QueueRequest(String requestData, RequestCallback callback) {
      this.requestData = requestData;
      this.callback = callback;
    }

    @Override
    public String getRequestString() {
      return requestData;
    }

    @Override
    public String getServiceName() {
      return serviceName;
    }

    @Override
    public void onAnswer(String answer) {
      removeRequest(this);
      finishAsking();
      if (!canceled)
        if (answer.startsWith("+")) {
          RequestHelper.process(callback, answer.substring(1));
        } else {
          callback.onError(null, new InternalServerException(answer.substring(1)));
        }
    }

    @Override
    public boolean onTimeout() {
      if (canceled) {
        removeRequest(this);
        finishAsking();
        return false;
      }

      if (!resendAllowed) {
        removeRequest(this);
        finishAsking();
        if (!canceled)
          callback.onError(null, new TimeoutException(resendAllowed));
      }
      return resendAllowed;
    }

    public Request getRequest() {
      return request;
    }

    public void send() {
      assert (!pending);
      pending = true;
      addRequest(this);
    }

    @Override
    public int compareTo(QueueRequest o) {
      return ((Double) priority).compareTo(o.priority);
    }
  }

  @Override
  public Request call(String requestData, RequestCallback requestCallback) {
    QueueRequest request = new QueueRequest(requestData, requestCallback);
    queue(request);
    return request.getRequest();
  }

  private void queue(QueueRequest request) {
    queued.add(request);

    askNext();
  }

  private int loadingCount = 0;

  private void finishAsking() {
    loadingCount--;
    askNext();
  }

  private void askNext() {
//    System.out.println("Queued: " + queued.size());
    scheduleFinaly(new ScheduledCommand() {
      @Override
      public void execute() {
        if (loadingCount < paralellRequests) {
          QueueRequest request = queued.poll();
          if (request != null) {
            loadingCount++;
            request.send();
          }
        }
      }
    });
  }

  private void cancelQueue(QueueRequest request) {
    queued.remove(request);
  }

  private final PriorityQueue<QueueRequest> queued = new PriorityQueue<QueueRequest>();

}
