// Copyright 2017, 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.steps;

import io.kubernetes.client.models.V1Pod;
import java.util.Map;
import oracle.kubernetes.operator.PodAwaiterStepFactory;
import oracle.kubernetes.operator.PodWatcher;
import oracle.kubernetes.operator.ProcessingConstants;
import oracle.kubernetes.operator.helpers.DomainPresenceInfo;
import oracle.kubernetes.operator.wlsconfig.WlsDomainConfig;
import oracle.kubernetes.operator.work.Component;
import oracle.kubernetes.operator.work.NextAction;
import oracle.kubernetes.operator.work.Packet;
import oracle.kubernetes.operator.work.Step;

public class WatchPodReadyAdminStep extends Step {
  private final Map<String, PodWatcher> podWatchers;

  public WatchPodReadyAdminStep(Map<String, PodWatcher> podWatchers, Step next) {
    super(next);
    this.podWatchers = podWatchers;
  }

  @Override
  public NextAction apply(Packet packet) {
    DomainPresenceInfo info = packet.getSPI(DomainPresenceInfo.class);
    WlsDomainConfig domainTopology =
        (WlsDomainConfig) packet.get(ProcessingConstants.DOMAIN_TOPOLOGY);
    V1Pod adminPod = info.getServers().get(domainTopology.getAdminServerName()).getPod().get();

    PodWatcher pw = podWatchers.get(adminPod.getMetadata().getNamespace());
    packet
        .getComponents()
        .put(
            ProcessingConstants.PODWATCHER_COMPONENT_NAME,
            Component.createFor(PodAwaiterStepFactory.class, pw));

    return doNext(pw.waitForReady(adminPod, getNext()), packet);
  }
}
