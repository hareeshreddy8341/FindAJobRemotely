import { Text, Title } from '@mantine/core';
import { Link } from 'react-router-dom';

export function HowItWorks() {
  return (
    <article className="max-w-2xl">
      <Title order={1}>How FindAJobRemotely works</Title>
      <Title order={2} size="h4" mt="xl">For candidates</Title>
      <Text mt="xs">
        Create a profile, add your skills and preferences, and browse remote jobs and internships. Every job shows how well
        you fit. Applying is free. If a company hires you through the platform, you owe a small commission, calculated as a
        percentage of the first month’s pay the company recorded. If nobody hires you, you pay nothing.
      </Text>
      <Title order={2} size="h4" mt="xl">For startups</Title>
      <Text mt="xs">
        Post roles, search public candidate profiles and project posts, and review applicants ranked by fit. When you
        record a hire, enter the agreed monthly pay so the commission can be calculated.
      </Text>
      <Title order={2} size="h4" mt="xl">Privacy</Title>
      <Text mt="xs">
        Your phone number, salary expectation and resume are encrypted and never shown on public pages. Employers see them
        only after you apply to one of their jobs. <Link to="/privacy">Read the details.</Link>
      </Text>
    </article>
  );
}

export function Privacy() {
  return (
    <article className="max-w-2xl">
      <Title order={1}>Privacy and security</Title>
      <Title order={2} size="h4" mt="xl">What is public</Title>
      <Text mt="xs">
        Your name, headline, skills, experience level, country, links and the posts you publish are visible to signed-in
        employers when your profile is public. You can switch your profile to private at any time.
      </Text>
      <Title order={2} size="h4" mt="xl">What is private</Title>
      <Text mt="xs">
        Email address, phone number, expected salary and resume. They are encrypted at rest. An employer can see them only
        for candidates who applied to that employer’s jobs. Administrators cannot see phone numbers, salary expectations or resumes.
      </Text>
      <Title order={2} size="h4" mt="xl">Your control</Title>
      <Text mt="xs">
        You can delete your account from Account and security. This removes your profile, resume, posts and applications.
      </Text>
    </article>
  );
}

export function NotFound() {
  return (
    <div className="py-16 text-center">
      <Title order={1}>Page not found</Title>
      <Text c="dimmed" mt="xs">The page you are looking for does not exist or has moved.</Text>
      <Link to="/" className="mt-4 inline-block font-semibold text-signal">Go to the home page</Link>
    </div>
  );
}
