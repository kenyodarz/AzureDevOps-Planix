import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./features/devops-agent/devops-agent.routes').then(m => m.devopsAgentRoutes)
  }
];

